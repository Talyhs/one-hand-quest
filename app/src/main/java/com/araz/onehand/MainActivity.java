package com.araz.onehand;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
  OilGame game;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(5, 12, 19));
    getWindow().setNavigationBarColor(Color.rgb(5, 12, 19));
    game = new OilGame(this); setContentView(game);
  }
  @Override protected void onPause() { super.onPause(); if(game!=null) game.pauseMusic(); }
  @Override protected void onResume() { super.onResume(); if(game!=null) game.resumeMusic(); }
  @Override protected void onDestroy() { if(game!=null) game.releaseAudio(); super.onDestroy(); }

  static class MusicLoop {
    private volatile boolean running=false, paused=false;
    private AudioTrack track; private Thread worker; private final int sampleRate=22050;
    private final double[] notes={392.00,493.88,587.33,493.88,440.00,392.00,329.63,392.00,293.66,392.00,440.00,493.88};
    void start() {
      if(running) return;
      try {
        int size=Math.max(AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT),4096);
        track=new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,size,AudioTrack.MODE_STREAM);
        track.setVolume(0.18f); track.play(); running=true;
        worker=new Thread(() -> {
          int idx=0;
          while(running) {
            if(paused) { try { Thread.sleep(80); } catch(InterruptedException ignored){} continue; }
            double freq=notes[idx++%notes.length]; int count=sampleRate/3; short[] pcm=new short[count];
            for(int i=0;i<count;i++) {
              double env=Math.min(1.0,i/500.0)*Math.min(1.0,(count-i)/900.0);
              double melody=Math.sin(2*Math.PI*freq*i/sampleRate)*0.10;
              double bass=Math.sin(2*Math.PI*(freq/2.0)*i/sampleRate)*0.045;
              pcm[i]=(short)(Math.max(-1,Math.min(1,(melody+bass)*env))*32767);
            }
            if(running && !paused && track!=null) track.write(pcm,0,pcm.length);
          }
        },"qara-qizil-music"); worker.setDaemon(true); worker.start();
      } catch(Exception ignored) { running=false; }
    }
    void setPaused(boolean value) {
      paused=value;
      try { if(track!=null) { if(value && track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING) track.pause(); else if(!value && track.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING) { track.flush(); track.play(); } } } catch(Exception ignored){}
    }
    void stop() { running=false; if(worker!=null) worker.interrupt(); try { if(track!=null) { track.stop(); track.release(); } } catch(Exception ignored){} track=null; }
  }

  static class Drop {
    float x, y, r, speed;
    boolean stone;
    Drop(float x, float y, float r, float speed, boolean stone) {
      this.x=x; this.y=y; this.r=r; this.speed=speed; this.stone=stone;
    }
  }

  class OilGame extends View {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random random = new Random();
    final ToneGenerator tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
    final MusicLoop music = new MusicLoop();
    boolean soundOn=true, musicOn=true, paused=false, settingsScreen=false, helpScreen=false;
    final ArrayList<Drop> drops = new ArrayList<>();
    final SharedPreferences prefs;
    final int gold = Color.rgb(255,190,62);
    final int cream = Color.rgb(255,235,177);
    float bucketX, bucketY, fingerX, lastX;
    int score=0, lives=3, level, coins, best, ticks=0, combo=0;
    boolean started=false, gameOver=false, levelWon=false;
    long lastFrame=System.currentTimeMillis();
    OilGame(Context context) {
      super(context);
      prefs=getSharedPreferences("black_gold_save", MODE_PRIVATE);
      level=Math.max(1, Math.min(313, prefs.getInt("level",1)));
      coins=prefs.getInt("coins",0);
      best=prefs.getInt("best",0);
      setLayerType(View.LAYER_TYPE_SOFTWARE,null); music.start();
    }
    String hearts() { String s=""; for(int i=0;i<Math.max(0,lives);i++) s += "♥ "; return s; }
    void playSound(int tone, int duration) { if(soundOn) try { tones.startTone(tone, duration); } catch (Exception ignored) {} }
    void pauseMusic(){ music.setPaused(true); }
    void resumeMusic(){ music.setPaused(!musicOn); }
    void releaseAudio(){ music.stop(); try { tones.release(); } catch(Exception ignored){} }
    void save() {
      prefs.edit().putInt("level",level).putInt("coins",coins).putInt("best",best).apply();
    }
    void startGame() {
      score=0; lives=3; ticks=0; combo=0; drops.clear(); started=true; gameOver=false; levelWon=false; paused=false; settingsScreen=false; helpScreen=false;
      music.setPaused(!musicOn); lastFrame=System.currentTimeMillis(); invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      float w=getWidth(), h=getHeight();
      if(w<=0 || h<=0) return;
      bucketY=h*.81f;
      drawBackground(canvas,w,h);
      drawOilRig(canvas,w,h);
      if(started && !gameOver && !levelWon && !paused && !settingsScreen && !helpScreen) {
        long now=System.currentTimeMillis();
        float dt=Math.min(.05f,Math.max(0f,(now-lastFrame)/1000f));
        lastFrame=now; ticks++;
        int spawnEvery=Math.max(19, fiftyFive()-level/3);
        if(ticks%spawnEvery==0) {
          float r=w*(.014f+random.nextFloat()*.008f);
          boolean stone=random.nextInt(100)<Math.min(28,8+level/6);
          drops.add(new Drop(w*.05f+random.nextFloat()*w*.90f,-r,r,h*(.28f+level*.004f+random.nextFloat()*.12f),stone));
        }
        Iterator<Drop> it=drops.iterator();
        while(it.hasNext()) {
          Drop d=it.next(); d.y+=d.speed*dt;
          drawDrop(canvas,d);
          if(d.y+d.r>bucketY-h*.02f && d.y-d.r<bucketY+h*.025f && Math.abs(d.x-bucketX)<w*.105f) {
            if(d.stone) { lives--; combo=0; score=Math.max(0,score-5); playSound(ToneGenerator.TONE_PROP_NACK,180); performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); }
            else { combo++; score+=10+Math.min(10,combo-1); coins++; best=Math.max(best,score); playSound(combo>3?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP2,90); performHapticFeedback(android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE); }
            it.remove();
          } else if(d.y>h+d.r) {
            if(!d.stone) { lives--; combo=0; }
            it.remove();
          }
        }
        if(score>=levelGoal()) {
          levelWon=true;
          if(level<313) level++;
          save();
        }
        if(lives<=0) { gameOver=true; playSound(ToneGenerator.TONE_PROP_NACK,380); save(); }
        postInvalidateDelayed(16);
      } else {
        for(Drop d:drops) drawDrop(canvas,d);
      }
      drawHUD(canvas,w,h);
      drawBucket(canvas,w,h);
      if(!started || gameOver || levelWon || paused || settingsScreen || helpScreen) drawPanel(canvas,w,h);
      else {
        p.setColor(0xffd5e1e9); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.032f);
        canvas.drawText("Qabı barmağınla sağa-sola sürüşdür",w/2,h*.94f,p);
      }
    }
    int fiftyFive(){ return Math.max(30,55-Math.min(20,level/6)); }
    int levelGoal(){ return 100 + (level-1)*8 + (level/25)*20; }
    void drawBackground(Canvas c,float w,float h) {
      p.setShader(new LinearGradient(0,0,0,h,0xff07111c,0xff163448,Shader.TileMode.CLAMP));
      c.drawRect(0,0,w,h,p); p.setShader(null);
      p.setShader(new RadialGradient(w*.82f,h*.16f,w*.10f,new int[]{0x55fff0bc,0x11fff0bc,0x00000000},null,Shader.TileMode.CLAMP));
      c.drawCircle(w*.82f,h*.16f,w*.10f,p); p.setShader(null);
      p.setColor(0x33f9d58a); c.drawCircle(w*.82f,h*.16f,w*.042f,p);
      for(int i=0;i<44;i++) {
        float sx=(i*83f+17)%w, sy=(i*151f)% (h*.62f);
        p.setColor((i%4==0)?0xffffd98b:0x667ca1b8);
        c.drawCircle(sx,sy,w*(i%4==0?.0022f:.0012f),p);
      }
      p.setColor(0xff0b1923);
      Path sea=new Path(); sea.moveTo(0,h*.68f);
      for(int xx=0;xx<=w+10;xx+=12) sea.lineTo(xx,h*.68f+(float)Math.sin(xx*.018)*h*.008f);
      sea.lineTo(w,h); sea.lineTo(0,h); sea.close(); c.drawPath(sea,p);
      p.setColor(0x3359a1b8); p.setStrokeWidth(1.5f);
      for(int i=0;i<5;i++) c.drawLine(0,h*(.72f+i*.045f),w,h*(.72f+i*.045f),p);
    }
    void drawOilRig(Canvas c,float w,float h) {
      p.setStrokeWidth(Math.max(3,w*.009f)); p.setStrokeCap(Paint.Cap.ROUND);
      p.setColor(0xff273b46);
      for(int i=0;i<4;i++) {
        float x=w*(.12f+i*.25f), y=h*.62f;
        c.drawLine(x,y,x+w*.035f,h*.51f,p); c.drawLine(x+w*.035f,h*.51f,x+w*.07f,y,p);
        c.drawLine(x+w*.017f,h*.57f,x+w*.055f,h*.57f,p);
        p.setColor(0xffc18a39); c.drawLine(x+w*.018f,h*.51f,x+w*.052f,h*.51f,p); p.setColor(0xff273b46);
      }
    }
    void drawDrop(Canvas c,Drop d) {
      if(d.stone) {
        p.setShader(new RadialGradient(d.x-d.r*.3f,d.y-d.r*.4f,d.r*1.5f,new int[]{0xffd1d8dc,0xff657783,0xff29343c},null,Shader.TileMode.CLAMP));
        c.drawCircle(d.x,d.y,d.r,p); p.setShader(null);
        p.setColor(0xff35434b); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
        c.drawLine(d.x-d.r*.4f,d.y-d.r*.1f,d.x+d.r*.35f,d.y+d.r*.35f,p); p.setStyle(Paint.Style.FILL);
      } else {
        p.setShadowLayer(12,0,0,0xfff3ad28);
        p.setShader(new RadialGradient(d.x-d.r*.3f,d.y-d.r*.4f,d.r*1.6f,new int[]{0xfffff1b5,0xffffc247,0xff9b5609},null,Shader.TileMode.CLAMP));
        Path path=new Path(); path.moveTo(d.x,d.y-d.r*1.35f); path.cubicTo(d.x+d.r*.15f,d.y-d.r*.55f,d.x+d.r*1.15f,d.y+d.r*.15f,d.x+d.r*.62f,d.y+d.r*.65f);
        path.cubicTo(d.x+d.r*.25f,d.y+d.r*1.15f,d.x-d.r*.45f,d.y+d.r*.95f,d.x-d.r*.68f,d.y+d.r*.45f);
        path.cubicTo(d.x-d.r*1.0f,d.y-d.r*.05f,d.x-d.r*.18f,d.y-d.r*.65f,d.x,d.y-d.r*1.35f);
        c.drawPath(path,p); p.setShader(null); p.clearShadowLayer();
        p.setColor(0xccfff7d1); p.setStrokeWidth(Math.max(1.5f,d.r*.10f)); p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(d.x-d.r*.18f,d.y-d.r*.50f,d.x-d.r*.30f,d.y-d.r*.18f,p);
        p.setColor(0x66fff1a0); c.drawCircle(d.x+d.r*.25f,d.y+d.r*.28f,d.r*.12f,p);
      }
    }
    void drawHUD(Canvas c,float w,float h) {
      p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL));
      p.setColor(cream); p.setTextSize(w*.065f); c.drawText("QARA QIZIL",w/2,h*.065f,p);
      if(started && !gameOver && !levelWon) {
        p.setColor(0xcc18303b); c.drawRoundRect(w*.865f,h*.035f,w*.965f,h*.105f,12,12,p);
        p.setColor(gold); p.setTextSize(w*.038f); c.drawText("Ⅱ",w*.915f,h*.083f,p);
      }
      p.setTypeface(Typeface.DEFAULT_BOLD); p.setColor(0xffb9ccd7); p.setTextSize(w*.031f); c.drawText("DAMCI OVÇUSU  •  SƏVİYYƏ "+level+"/313",w/2,h*.103f,p);
      p.setTextAlign(Paint.Align.LEFT); p.setColor(gold); p.setTextSize(w*.041f); c.drawText("XAL  "+score,w*.055f,h*.155f,p);
      p.setTextAlign(Paint.Align.RIGHT); p.setColor(0xffff7a76); c.drawText(hearts(),w*.945f,h*.155f,p);
      p.setTextAlign(Paint.Align.CENTER); p.setColor(0xffc5d7e2); p.setTextSize(w*.029f); c.drawText("Sikkə: "+coins+"     Rekord: "+best+(combo>1?"     KOMBO x"+combo:""),w/2,h*.19f,p);
    }
    void drawBucket(Canvas c,float w,float h) {
      if(bucketX==0) bucketX=w/2;
      float bw=w*.19f, bh=h*.055f, x=bucketX, y=bucketY;
      p.setShadowLayer(14,0,0,0xffe6a52c); p.setColor(0xffd49a38);
      Path b=new Path(); b.moveTo(x-bw/2,y-bh/2); b.lineTo(x+bw/2,y-bh/2); b.lineTo(x+bw*.36f,y+bh/2); b.quadTo(x,y+bh*.75f,x-bw*.36f,y+bh/2); b.close(); c.drawPath(b,p); p.clearShadowLayer();
      p.setColor(cream); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(3,w*.008f)); c.drawLine(x-bw*.56f,y-bh*.52f,x+bw*.56f,y-bh*.52f,p); p.setStyle(Paint.Style.FILL);
      p.setColor(0xff70400b); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.022f); c.drawText("NEFT",x,y+bh*.20f,p);
      p.setColor(0x99fff0b0); p.setStrokeWidth(Math.max(1.5f,w*.003f)); c.drawLine(x-bw*.30f,y+bh*.05f,x+bw*.28f,y+bh*.05f,p);
    }
    void drawButton(Canvas c,float w,float y1,float y2,String label,boolean primary) {
      p.setColor(primary?gold:0xff234452); c.drawRoundRect(w*.24f,y1,w*.76f,y2,18,18,p);
      p.setColor(primary?0xff201707:0xffe4edf2); p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL));
      p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.038f); c.drawText(label,w/2,(y1+y2)*.5f+w*.013f,p);
    }
    void drawPanel(Canvas c,float w,float h) {
      p.setColor(0xb900080e); c.drawRect(0,0,w,h,p);
      float top=(settingsScreen||helpScreen)?.30f:.27f, bottom=!started?.84f:.74f;
      if(paused) { top=.32f; bottom=.71f; }
      p.setColor(0xf0132732); c.drawRoundRect(w*.065f,h*top,w*.935f,h*bottom,28,28,p);
      p.setColor(0xffd6a34a); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
      c.drawRoundRect(w*.065f,h*top,w*.935f,h*bottom,28,28,p); p.setStyle(Paint.Style.FILL);
      p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL));
      p.setColor(gold); p.setTextSize(w*.065f);
      String title=settingsScreen?"TƏNZİMLƏMƏLƏR":helpScreen?"NECƏ OYNANIR":paused?"FASİLƏ":!started?"QARA QIZIL":levelWon?"SƏVİYYƏ KEÇİLDİ":"OYUN BİTDİ";
      c.drawText(title,w/2,h*(top+.10f),p); p.setTypeface(Typeface.DEFAULT); p.setColor(0xffe4edf2); p.setTextSize(w*.034f);
      if(settingsScreen) {
        c.drawText("Fon musiqisi: "+(musicOn?"AÇIQ":"SÖNÜLÜ"),w/2,h*.455f,p);
        drawButton(c,w,h*.49f,h*.55f,musicOn?"MUSİQİNİ SÖNDÜR":"MUSİQİNİ AÇ",true);
        c.drawText("Səs effektləri: "+(soundOn?"AÇIQ":"SÖNÜLÜ"),w/2,h*.595f,p);
        drawButton(c,w,h*.62f,h*.68f,soundOn?"SƏSİ SÖNDÜR":"SƏSİ AÇ",false);
        drawButton(c,w,h*.70f,h*.76f,"GERİ",false);
      } else if(helpScreen) {
        c.drawText("Qızılı neft damcılarını tut.",w/2,h*.455f,p);
        c.drawText("Boz daşlardan yayın — canını itirmə.",w/2,h*.505f,p);
        c.drawText("Qabı bir barmaqla sağa-sola sürüşdür.",w/2,h*.555f,p);
        c.drawText("Ardıcıl damcılar KOMBO bonusu verir.",w/2,h*.605f,p);
        drawButton(c,w,h*.68f,h*.74f,"GERİ",true);
      } else if(paused) {
        c.drawText("Oyun dayandırılıb.",w/2,h*.47f,p);
        drawButton(c,w,h*.515f,h*.585f,"DAVAM ET",true); drawButton(c,w,h*.615f,h*.685f,"ƏSAS MENYU",false);
      } else if(!started) {
        c.drawText("Qızılı damcıları tut, daşlardan yayın!",w/2,h*.425f,p);
        c.drawText("313 səviyyə • Kombo • Rekord • Sikkələr",w/2,h*.47f,p);
        c.drawText("Bir barmaqla rahat oyna.",w/2,h*.515f,p);
        drawButton(c,w,h*.56f,h*.625f,"OYUNA BAŞLA",true);
        drawButton(c,w,h*.65f,h*.71f,"TƏNZİMLƏMƏLƏR",false); drawButton(c,w,h*.735f,h*.795f,"NECƏ OYNANIR",false);
      } else if(levelWon) {
        c.drawText("Əla! Növbəti səviyyə: "+level,w/2,h*.47f,p); c.drawText("Xal: "+score+"   •   Rekord: "+best,w/2,h*.52f,p);
        drawButton(c,w,h*.575f,h*.635f,"DAVAM ET",true); drawButton(c,w,h*.655f,h*.715f,"ƏSAS MENYU",false);
      } else {
        c.drawText("Topladığın xal: "+score,w/2,h*.47f,p); c.drawText("Rekord: "+best+"   •   Sikkə: "+coins,w/2,h*.52f,p);
        c.drawText("Kombonu artır, rekordu qır!",w/2,h*.56f,p);
        drawButton(c,w,h*.59f,h*.65f,"YENİDƏN OYNA",true); drawButton(c,w,h*.67f,h*.73f,"ƏSAS MENYU",false);
      }
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
      float w=getWidth(), h=getHeight();
      if(e.getAction()==MotionEvent.ACTION_DOWN) { fingerX=e.getX(); lastX=fingerX; return true; }
      if(e.getAction()==MotionEvent.ACTION_MOVE) {
        if(started && !gameOver && !levelWon && !paused && !settingsScreen && !helpScreen)
          bucketX=Math.max(w*.11f,Math.min(w*.89f,e.getX()));
        invalidate(); return true;
      }
      if(e.getAction()==MotionEvent.ACTION_UP) {
        float x=e.getX(), y=e.getY();
        if(started && !gameOver && !levelWon && !paused && !settingsScreen && !helpScreen && x>w*.84f && y<h*.13f) {
          paused=true; music.setPaused(true); invalidate(); return true;
        }
        if(settingsScreen) {
          if(y>h*.48f && y<h*.57f) { musicOn=!musicOn; music.setPaused(!musicOn); }
          else if(y>h*.60f && y<h*.69f) soundOn=!soundOn;
          else if(y>h*.69f && y<h*.78f) settingsScreen=false;
        } else if(helpScreen) {
          if(y>h*.66f && y<h*.77f) helpScreen=false;
        } else if(paused) {
          if(y>h*.50f && y<h*.60f) { paused=false; music.setPaused(!musicOn); lastFrame=System.currentTimeMillis(); }
          else if(y>h*.60f && y<h*.71f) { paused=false; started=false; gameOver=false; levelWon=false; music.setPaused(!musicOn); }
        } else if(!started) {
          if(y>h*.54f && y<h*.64f) startGame();
          else if(y>h*.64f && y<h*.725f) settingsScreen=true;
          else if(y>h*.725f && y<h*.81f) helpScreen=true;
        } else if(gameOver || levelWon) {
          if(y>h*.56f && y<h*.66f) { if(level>=313 && levelWon) level=1; startGame(); }
          else if(y>h*.65f && y<h*.76f) { started=false; gameOver=false; levelWon=false; music.setPaused(!musicOn); }
        } else if(started && !paused && !settingsScreen && !helpScreen) {
          bucketX=Math.max(w*.11f,Math.min(w*.89f,x));
        }
        invalidate(); return true;
      }
      return true;
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
      super.onSizeChanged(w,h,oldw,oldh);
      if(bucketX<=0 || bucketX>w) bucketX=w/2f;
      bucketY=h*.81f;
    }
  }
}