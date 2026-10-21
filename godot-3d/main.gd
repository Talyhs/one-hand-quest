extends Node3D

const MAX_LEVEL := 999
const SAVE_PATH := "user://qara_qizil_3d.cfg"

var camera: Camera3D
var bucket: Node3D
var drops: Array[Dictionary] = []
var ui: CanvasLayer
var stats_label: Label
var hint_label: Label
var start_button: Button
var panel: PanelContainer
var score := 0
var lives := 3
var level := 1
var coins := 0
var best := 0
var combo := 0
var playing := false
var level_won := false
var spawn_clock := 0.0
var audio_player: AudioStreamPlayer
var audio_playback: AudioStreamGeneratorPlayback
var audio_phase := 0.0

func _ready() -> void:
	_load_save()
	_build_world()
	_build_ui()
	_update_ui()

func _material(color: Color, metallic: float = 0.0, roughness: float = 0.5, glow: bool = false) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = color
	m.metallic = metallic
	m.roughness = roughness
	if glow:
		m.emission_enabled = true
		m.emission = color
		m.emission_energy_multiplier = 1.4
	return m

func _box(label: String, pos: Vector3, size: Vector3, color: Color, parent: Node3D = self) -> MeshInstance3D:
	var n := MeshInstance3D.new()
	n.name = label
	var mesh := BoxMesh.new()
	mesh.size = size
	n.mesh = mesh
	n.position = pos
	n.material_override = _material(color, 0.25, 0.4)
	parent.add_child(n)
	return n

func _build_world() -> void:
	var we := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.015, 0.035, 0.055)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.43, 0.55, 0.68)
	env.ambient_light_energy = 0.7
	we.environment = env
	add_child(we)

	camera = Camera3D.new()
	camera.position = Vector3(0, 7.8, 14.5)
	camera.look_at(Vector3(0, 2.5, 0), Vector3.UP)
	camera.current = true
	camera.fov = 48.0
	add_child(camera)

	var light := DirectionalLight3D.new()
	light.rotation_degrees = Vector3(-52, -28, 0)
	light.light_energy = 1.35
	light.shadow_enabled = true
	add_child(light)
	var glow := OmniLight3D.new()
	glow.position = Vector3(0, 5, 3)
	glow.light_color = Color(1.0, 0.63, 0.23)
	glow.light_energy = 2.2
	glow.omni_range = 18
	add_child(glow)

	_box("Platform", Vector3(0, -0.45, 0), Vector3(11, 0.5, 8), Color(0.07, 0.12, 0.15))
	_box("Back wall", Vector3(0, 2.3, -4.1), Vector3(11, 5.5, 0.3), Color(0.035, 0.075, 0.095))
	for i in range(5):
		var x := -4.4 + float(i) * 2.2
		_box("Rig leg %d" % i, Vector3(x, 1.3, -2.8), Vector3(0.16, 3.2, 0.18), Color(0.22, 0.29, 0.31))
		_box("Rig brace %d" % i, Vector3(x + 0.45, 1.7, -2.8), Vector3(1.0, 0.09, 0.14), Color(0.66, 0.39, 0.12))
	_box("Rig top beam", Vector3(0, 3.0, -2.8), Vector3(10.1, 0.22, 0.24), Color(0.74, 0.47, 0.17))

	bucket = Node3D.new()
	bucket.position = Vector3(0, 0.2, 2.5)
	add_child(bucket)
	var body := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.75
	cyl.bottom_radius = 0.53
	cyl.height = 0.75
	body.mesh = cyl
	body.material_override = _material(Color(0.69, 0.39, 0.1), 0.72, 0.28)
	bucket.add_child(body)
	var rim := MeshInstance3D.new()
	var torus := TorusMesh.new()
	torus.inner_radius = 0.65
	torus.outer_radius = 0.75
	rim.mesh = torus
	rim.position.y = 0.39
	rim.material_override = _material(Color(1.0, 0.72, 0.25), 0.7, 0.2, true)
	bucket.add_child(rim)
	_box("Bucket badge", Vector3(0, 0.2, 0.43), Vector3(0.42, 0.05, 0.05), Color(1.0, 0.8, 0.34), bucket)

	var stream := AudioStreamGenerator.new()
	stream.mix_rate = 22050
	stream.buffer_length = 0.18
	audio_player = AudioStreamPlayer.new()
	audio_player.stream = stream
	add_child(audio_player)
	audio_player.play()
	audio_playback = audio_player.get_stream_playback() as AudioStreamGeneratorPlayback

func _build_ui() -> void:
	ui = CanvasLayer.new()
	add_child(ui)
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(root)

	var title := Label.new()
	title.text = "QARA QIZIL  •  3D"
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	title.add_theme_font_size_override("font_size", 30)
	title.add_theme_color_override("font_color", Color(1.0, 0.75, 0.28))
	title.anchor_left = 0.04
	title.anchor_right = 0.96
	title.anchor_top = 0.025
	title.anchor_bottom = 0.08
	root.add_child(title)

	stats_label = Label.new()
	stats_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	stats_label.add_theme_font_size_override("font_size", 17)
	stats_label.add_theme_color_override("font_color", Color(0.84, 0.91, 0.94))
	stats_label.anchor_left = 0.03
	stats_label.anchor_right = 0.97
	stats_label.anchor_top = 0.09
	stats_label.anchor_bottom = 0.15
	root.add_child(stats_label)

	hint_label = Label.new()
	hint_label.text = "Damcıları tut • boz daşlardan yayın"
	hint_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	hint_label.add_theme_font_size_override("font_size", 16)
	hint_label.add_theme_color_override("font_color", Color(0.76, 0.84, 0.88))
	hint_label.anchor_left = 0.04
	hint_label.anchor_right = 0.96
	hint_label.anchor_top = 0.87
	hint_label.anchor_bottom = 0.92
	root.add_child(hint_label)

	panel = PanelContainer.new()
	panel.anchor_left = 0.09
	panel.anchor_right = 0.91
	panel.anchor_top = 0.32
	panel.anchor_bottom = 0.67
	var style := StyleBoxFlat.new()
	style.bg_color = Color(0.025, 0.065, 0.09, 0.96)
	style.border_color = Color(1.0, 0.68, 0.2)
	style.set_border_width_all(2)
	style.set_corner_radius_all(20)
	style.content_margin_left = 18
	style.content_margin_right = 18
	style.content_margin_top = 18
	style.content_margin_bottom = 18
	panel.add_theme_stylebox_override("panel", style)
	root.add_child(panel)
	var col := VBoxContainer.new()
	col.alignment = BoxContainer.ALIGNMENT_CENTER
	col.add_theme_constant_override("separation", 16)
	panel.add_child(col)
	var panel_title := Label.new()
	panel_title.name = "PanelTitle"
	panel_title.text = "Qara Qızıl: Damcı Ovçusu"
	panel_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	panel_title.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	panel_title.add_theme_font_size_override("font_size", 23)
	panel_title.add_theme_color_override("font_color", Color(1.0, 0.75, 0.28))
	col.add_child(panel_title)
	var body := Label.new()
	body.name = "PanelBody"
	body.text = "Qızılı neft damcılarını tut. Boz daşlardan yayın. Bir barmaqla idarə et!"
	body.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	body.add_theme_font_size_override("font_size", 17)
	col.add_child(body)
	start_button = Button.new()
	start_button.custom_minimum_size = Vector2(0, 58)
	start_button.text = "OYUNA BAŞLA"
	start_button.add_theme_font_size_override("font_size", 20)
	start_button.add_theme_color_override("font_color", Color(0.08, 0.06, 0.025))
	var bs := StyleBoxFlat.new()
	bs.bg_color = Color(1.0, 0.72, 0.24)
	bs.set_corner_radius_all(14)
	start_button.add_theme_stylebox_override("normal", bs)
	start_button.add_theme_stylebox_override("hover", bs)
	start_button.add_theme_stylebox_override("pressed", bs)
	start_button.pressed.connect(_on_start_pressed)
	col.add_child(start_button)

func _process(delta: float) -> void:
	if not playing:
		return
	spawn_clock += delta
	var interval := maxf(0.32, 0.95 - float(level) * 0.0012)
	if spawn_clock >= interval:
		spawn_clock = 0.0
		_spawn_drop()
	for i in range(drops.size() - 1, -1, -1):
		var d: Dictionary = drops[i]
		var node: Node3D = d.node
		node.position.y -= float(d.speed) * delta
		node.rotate_y(delta * (2.0 if not d.stone else 0.8))
		if node.position.y <= 0.78:
			if absf(node.position.x - bucket.position.x) < 0.92 and absf(node.position.z - bucket.position.z) < 1.0:
				if d.stone:
					lives -= 1
					score = maxi(0, score - 5)
					combo = 0
					_tone(180.0, 0.12, 0.2)
				else:
					combo += 1
					score += 10 + mini(10, combo - 1)
					coins += 1
					best = maxi(best, score)
					_tone(520.0 + float(mini(combo, 8)) * 55.0, 0.075, 0.12)
				node.queue_free()
				drops.remove_at(i)
				_update_ui()
				if lives <= 0:
					_finish_game(false)
					return
			elif node.position.y < 0.55:
				if not d.stone:
					lives -= 1
					combo = 0
				node.queue_free()
				drops.remove_at(i)
				if lives <= 0:
					_finish_game(false)
					return
		elif node.position.y < -0.5:
			node.queue_free()
			drops.remove_at(i)
	if score >= _level_goal():
		_finish_game(true)
	_update_ui()

func _spawn_drop() -> void:
	var stone := randi_range(0, 99) < mini(30, 8 + int(level / 7))
	var n := Node3D.new()
	n.position = Vector3(randf_range(-4.3, 4.3), 7.2, randf_range(-1.8, 1.5))
	add_child(n)
	var mesh_node := MeshInstance3D.new()
	var mesh := SphereMesh.new()
	mesh.radius = 0.34 if not stone else 0.32
	mesh.height = 0.68 if not stone else 0.64
	mesh_node.mesh = mesh
	if stone:
		mesh_node.scale = Vector3(randf_range(0.8, 1.25), randf_range(0.8, 1.2), 1.0)
		mesh_node.material_override = _material(Color(0.36, 0.43, 0.47), 0.18, 0.72)
	else:
		mesh_node.scale = Vector3(0.8, 1.25, 0.8)
		mesh_node.material_override = _material(Color(1.0, 0.61, 0.12), 0.52, 0.2, true)
		var lamp := OmniLight3D.new()
		lamp.light_color = Color(1.0, 0.58, 0.12)
		lamp.light_energy = 0.65
		lamp.omni_range = 2.0
		n.add_child(lamp)
	n.add_child(mesh_node)
	var speed := 3.0 + minf(float(level) * 0.008, 2.4) + randf_range(0.0, 1.0)
	drops.append({"node": n, "stone": stone, "speed": speed})

func _level_goal() -> int:
	return 100 + (level - 1) * 8 + int(level / 25) * 20

func _on_start_pressed() -> void:
	if level_won and level >= MAX_LEVEL:
		level = 1
		score = 0
	playing = true
	level_won = false
	lives = 3
	combo = 0
	spawn_clock = 0.0
	for d in drops:
		if is_instance_valid(d.node):
			d.node.queue_free()
	drops.clear()
	panel.visible = false
	hint_label.text = "Qabını barmağınla sola-sağa sürüşdür"
	_update_ui()

func _finish_game(won: bool) -> void:
	playing = false
	level_won = won
	if won and level < MAX_LEVEL:
		level += 1
	best = maxi(best, score)
	_save_game()
	panel.visible = true
	var t: Label = panel.find_child("PanelTitle", true, false)
	var b: Label = panel.find_child("PanelBody", true, false)
	if won:
		t.text = "ƏFSANƏSƏN!" if level >= MAX_LEVEL else "SƏVİYYƏ KEÇİLDİ!"
		b.text = "999 səviyyənin hamısını tamamladın!" if level >= MAX_LEVEL else "Növbəti səviyyə: %d\nXal: %d\nKombo: x%d" % [level, score, combo]
		start_button.text = "YENİDƏN BAŞLA" if level >= MAX_LEVEL else "DAVAM ET"
	else:
		t.text = "OYUN BİTDİ"
		b.text = "Xal: %d\nRekord: %d\nSikkə: %d" % [score, best, coins]
		start_button.text = "YENİDƏN OYNA"
	_update_ui()

func _update_ui() -> void:
	if stats_label:
		stats_label.text = "SƏVİYYƏ %d/%d     XAL %d     ♥ %d\nSİKKƏ %d     REKORD %d%s" % [level, MAX_LEVEL, score, lives, coins, best, ("     KOMBO x%d" % combo) if combo > 1 else ""]

func _input(event: InputEvent) -> void:
	if not playing:
		return
	var x := -999.0
	if event is InputEventScreenTouch and event.pressed:
		x = event.position.x
	elif event is InputEventScreenDrag:
		x = event.position.x
	elif event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		x = event.position.x
	elif event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
		x = event.position.x
	if x >= 0:
		var width := maxf(1.0, float(get_viewport().get_visible_rect().size.x))
		bucket.position.x = clampf((x / width - 0.5) * 9.0, -4.2, 4.2)

func _tone(freq: float, duration: float, volume: float) -> void:
	if audio_playback == null:
		return
	var count := mini(int(22050.0 * duration), 2205)
	for i in range(count):
		if not audio_playback.can_push_buffer(1):
			break
		var sample := sin(audio_phase) * volume * (1.0 - float(i) / float(maxi(1, count)))
		audio_playback.push_frame(Vector2(sample, sample))
		audio_phase += TAU * freq / 22050.0

func _save_game() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("progress", "level", level)
	cfg.set_value("progress", "coins", coins)
	cfg.set_value("progress", "best", best)
	cfg.save(SAVE_PATH)

func _load_save() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(SAVE_PATH) == OK:
		level = clampi(int(cfg.get_value("progress", "level", 1)), 1, MAX_LEVEL)
		coins = maxi(0, int(cfg.get_value("progress", "coins", 0)))
		best = maxi(0, int(cfg.get_value("progress", "best", 0)))
