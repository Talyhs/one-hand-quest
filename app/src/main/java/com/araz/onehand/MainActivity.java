package com.araz.onehand;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import java.util.*;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(0xff06131e);
    getWindow().setNavigationBarColor(0xff06131e);
    setContentView(new Game(this));
  }
  static class Drop { int lane; float y; boolean rock; Drop(int l,float y,boolean r){lane=l;this.y=y;rock=r;} }
  class Game extends View {
    Paint p=new Paint(3); Random rnd=new Random(); SharedPreferences pref;
    ArrayList<Drop> drops=new ArrayList<>(); ToneGenerator tone;
    int level,coins,score,lives,lane=1,ticks,screen=0,theme=0; // menu, game, levels, settings, help, pause, result
    boolean sound=true,music=true; float downX,downY; long last=System.currentTimeMillis();
    int gold=0xffffbd27,cream=0xffffedb0,blue=0xff4bd9f2;
    Game(Context c){super(c);pref=getSharedPreferences("qara_qizil",0);level=Math.max(1,Math.min(999,pref.getInt("level",1)));coins=pref.getInt("coins",0);sound=pref.getBoolean("sound",true);music=pref.getBoolean("music",true);try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,78);}catch(Exception e){}setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void sfx(int t){if(sound&&tone!=null)try{tone.startTone(t,110);}catch(Exception e){}}
    String hearts(){StringBuilder b=new StringBuilder();for(int i=0;i<lives;i++)b.append("♥ ");return b.toString();}
    void save(){pref.edit().putInt("level",level).putInt("coins",coins).putBoolean("sound",sound).putBoolean("music",music).apply();}
    void start(){score=0;lives=3;lane=1;ticks=0;drops.clear();screen=1;last=System.currentTimeMillis();sfx(ToneGenerator.TONE_PROP_ACK);invalidate();}
    void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif-medium",0));p.clearShadowLayer();c.drawText(s,x,y,p);}
    void panel(Canvas c,float l,float t,float r,float b){p.setShader(new LinearGradient(l,t,r,b,0xee061520,0xee123747,Shader.TileMode.CLAMP));c.drawRoundRect(l,t,r,b,24,24,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(gold);c.drawRoundRect(l,t,r,b,24,24,p);p.setStyle(Paint.Style.FILL);}
    void btn(Canvas c,String s,float x,float y,float w,float h,boolean primary){float l=x-w/2,t=y-h/2;p.setShader(new LinearGradient(l,t,l,t+h,primary?0xffffe75b:0xff174052,primary?0xffff8700:0xff071d2b,Shader.TileMode.CLAMP));c.drawRoundRect(l,t,l+w,t+h,h/2,h/2,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(primary?0xffffefa2:0xff5c9eb0);c.drawRoundRect(l,t,l+w,t+h,h/2,h/2,p);p.setStyle(Paint.Style.FILL);txt(c,s,x,y+h*.13f,h*.35f,primary?0xff251500:0xfff2f8fa,true);}
    void background(Canvas c,float w,float h){int[][] palettes={{0xff041321,0xff1c2a43,0xffff8b27},{0xff07152b,0xff301b56,0xffed43ad},{0xff09233a,0xff126b85,0xffb9efff},{0xff061d17,0xff145a3c,0xffb6e45c},{0xff030719,0xff21134e,0xff6c70ff}};int[] a=palettes[theme%palettes.length];p.setShader(new LinearGradient(0,0,w,h,a[0],a[1],Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
      p.setShader(new RadialGradient(w*.5f,h*.30f,w*.68f,new int[]{(a[2]&0x00ffffff)|0x55000000,0x00000000},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
      for(int i=0;i<55;i++){float x=(i*137%997)/997f*w,y=(i*193%997)/997f*h*.75f;p.setColor((i%3==0?0x99ffcf68:0x4478dfff));c.drawCircle(x,y,i%3+0.7f,p);}
      // stylized oil derricks and horizon
      p.setColor(0x665b8391);p.setStrokeWidth(3);if(theme==0){for(int i=0;i<5;i++){float x=w*(.04f+i*.22f),y=h*.30f;c.drawLine(x,y,x+w*.035f,y-h*.13f,p);c.drawLine(x+w*.07f,y,x+w*.035f,y-h*.13f,p);c.drawLine(x,y,x+w*.07f,y,p);c.drawLine(x+w*.02f,y-h*.055f,x+w*.05f,y-h*.055f,p);}}
      else if(theme==1){for(int i=0;i<9;i++){float x=i*w/9f,b=h*(.42f+(i%3)*.035f);p.setColor(i%2==0?0xff101b38:0xff211747);c.drawRect(x,b,x+w*.085f,h*.80f,p);p.setColor(0xfffc4fc8);for(int j=0;j<4;j++)c.drawRect(x+w*.018f,b+h*(.035f+j*.045f),x+w*.027f,b+h*(.055f+j*.045f),p);p.setColor(0xff27cfff);for(int j=0;j<3;j++)c.drawRect(x+w*.052f,b+h*(.055f+j*.05f),x+w*.061f,b+h*(.075f+j*.05f),p);}}
      else if(theme==2){p.setColor(0xff527b9a);Path m=new Path();m.moveTo(0,h*.65f);m.lineTo(w*.22f,h*.26f);m.lineTo(w*.38f,h*.55f);m.lineTo(w*.60f,h*.20f);m.lineTo(w*.83f,h*.58f);m.lineTo(w,h*.34f);m.lineTo(w,h*.82f);m.lineTo(0,h*.82f);m.close();c.drawPath(m,p);p.setColor(0xffd9f3ff);Path snow=new Path();snow.moveTo(w*.22f,h*.26f);snow.lineTo(w*.16f,h*.38f);snow.lineTo(w*.22f,h*.35f);snow.lineTo(w*.28f,h*.40f);snow.close();c.drawPath(snow,p);snow.reset();snow.moveTo(w*.60f,h*.20f);snow.lineTo(w*.52f,h*.36f);snow.lineTo(w*.60f,h*.32f);snow.lineTo(w*.68f,h*.39f);snow.close();c.drawPath(snow,p);}
      else if(theme==3){p.setColor(0xff103d2a);c.drawRect(0,h*.60f,w,h*.82f,p);p.setColor(0xff5be7df);c.drawRect(w*.46f,h*.38f,w*.56f,h*.78f,p);for(int i=0;i<30;i++){float x=(i*83%997)/997f*w,y=h*(.25f+(i*37%45)/100f);p.setColor(i%2==0?0xff247d43:0xff4caa49);c.drawCircle(x,y,w*(.025f+(i%3)*.008f),p);p.setColor(0x99e3ff85);c.drawCircle(x-w*.01f,y-h*.008f,w*.008f,p);}}
      else {p.setColor(0xff121d49);c.drawCircle(w*.75f,h*.36f,w*.16f,p);p.setColor(0xff4b62ca);c.drawCircle(w*.72f,h*.33f,w*.14f,p);p.setColor(0xffa06aff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);c.drawOval(w*.56f,h*.30f,w*.92f,h*.42f,p);p.setStyle(Paint.Style.FILL);p.setColor(0xff69d9ff);c.drawCircle(w*.20f,h*.48f,w*.045f,p);p.setColor(0xfff2a4ff);c.drawCircle(w*.37f,h*.35f,w*.018f,p);}
    }
    @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;background(c,w,h);
      if(screen==0){txt(c,"QARA QIZIL",w/2,h*.12f,w*.105f,gold,true);txt(c,"DAMCI OVÇUSU",w/2,h*.17f,w*.045f,cream,true);
        p.setShadowLayer(25,0,0,gold);p.setShader(new LinearGradient(w*.43f,h*.21f,w*.57f,h*.42f,0xfffff6b5,0xffff8700,Shader.TileMode.CLAMP));Path d=new Path();d.moveTo(w*.5f,h*.21f);d.cubicTo(w*.47f,h*.29f,w*.42f,h*.32f,w*.44f,h*.37f);d.cubicTo(w*.46f,h*.43f,w*.54f,h*.43f,w*.56f,h*.37f);d.cubicTo(w*.58f,h*.32f,w*.53f,h*.27f,w*.5f,h*.21f);c.drawPath(d,p);p.clearShadowLayer();p.setShader(null);
        panel(c,w*.09f,h*.44f,w*.91f,h*.94f);btn(c,"▶   OYUNA BAŞLA",w/2,h*.52f,w*.72f,h*.075f,true);btn(c,"▦   SƏVİYYƏLƏR",w/2,h*.615f,w*.72f,h*.063f,false);btn(c,"⚙   TƏNZİMLƏMƏLƏR",w/2,h*.695f,w*.72f,h*.063f,false);btn(c,"♫   NECƏ OYNANIR?",w/2,h*.775f,w*.72f,h*.063f,false);txt(c,"999 SƏVİYYƏ • "+coins+" SİKKƏ",w/2,h*.86f,w*.035f,cream,true);txt(c,"Kiçik damlalar, böyük gələcək!",w/2,h*.91f,w*.032f,gold,false);return;}
      if(screen==2||screen==3||screen==4){panel(c,w*.07f,h*.10f,w*.93f,h*.90f);txt(c,screen==2?"SƏVİYYƏ SEÇİMİ":screen==3?"TƏNZİMLƏMƏLƏR":"NECƏ OYNANIR?",w/2,h*.17f,w*.06f,cream,true);
        if(screen==2){txt(c,"İRƏLİLƏYİŞ: "+level+" / 999",w/2,h*.25f,w*.04f,gold,true);for(int i=0;i<12;i++){int lv=Math.max(1,Math.min(999,level-5+i));float x=w*(.20f+(i%3)*.30f),y=h*(.34f+(i/3)*.105f);btn(c,""+lv,x,y,w*.22f,h*.065f,lv==level);}txt(c,"HƏR QƏLƏBƏ SƏNİ DAHA GÜCLÜ EDİR",w/2,h*.79f,w*.027f,cream,false);}
        if(screen==3){txt(c,"Səs effektləri: "+(sound?"AÇIQ":"SÖNÜLÜ"),w/2,h*.32f,w*.042f,cream,true);btn(c,"SƏSİ DƏYİŞ",w/2,h*.42f,w*.64f,h*.07f,true);txt(c,"Musiqi: "+(music?"AÇIQ":"SÖNÜLÜ"),w/2,h*.55f,w*.042f,cream,true);btn(c,"MUSİQİNİ DƏYİŞ",w/2,h*.65f,w*.64f,h*.07f,false);txt(c,"Fon üslubu: "+new String[]{"Neft sahəsi","Neon şəhər","Buz dağları","Yaşıl təbiət","Kosmos"}[theme],w/2,h*.75f,w*.03f,cream,false);btn(c,"FONU DƏYİŞ",w/2,h*.80f,w*.60f,h*.055f,false);}
        if(screen==4){txt(c,"QIZILI DAMLALARI TUT",w/2,h*.34f,w*.045f,gold,true);txt(c,"Daşlardan yayın, xal və sikkə topla.",w/2,h*.43f,w*.034f,cream,false);txt(c,"Sola və sağa toxun, yaxud sürüşdür.",w/2,h*.50f,w*.031f,cream,false);txt(c,"3 canın var. Səviyyə artdıqca sürət artır.",w/2,h*.57f,w*.029f,cream,false);txt(c,"999-cu səviyyəyə çata bilərsən?",w/2,h*.65f,w*.036f,gold,true);}
        btn(c,"← GERİ",w/2,h*.85f,w*.55f,h*.065f,false);return;}
      if(screen==5){panel(c,w*.10f,h*.34f,w*.90f,h*.68f);txt(c,"PAUZA",w/2,h*.43f,w*.09f,gold,true);btn(c,"▶  DAVAM ET",w/2,h*.53f,w*.68f,h*.075f,true);btn(c,"⌂  ƏSAS MENYU",w/2,h*.62f,w*.68f,h*.065f,false);return;}
      if(screen==6){panel(c,w*.08f,h*.32f,w*.92f,h*.70f);txt(c,lives<=0?"YENİDƏN CƏHD ET!":"SƏVİYYƏ TAMAMLANDI!",w/2,h*.42f,w*.052f,gold,true);txt(c,"XAL: "+score+"     SİKKƏ: "+coins,w/2,h*.50f,w*.04f,cream,true);btn(c,lives<=0?"YENİDƏN OYNA":"NÖVBƏTİ SƏVİYYƏ",w/2,h*.59f,w*.72f,h*.075f,true);btn(c,"ƏSAS MENYU",w/2,h*.66f,w*.60f,h*.06f,false);return;}
      // Game scene
      txt(c,"QARA QIZIL",w/2,h*.065f,w*.052f,gold,true);txt(c,"SƏVİYYƏ "+level+" / 999",w/2,h*.105f,w*.032f,cream,true);txt(c,"◆ "+coins,w*.20f,h*.16f,w*.038f,gold,true);txt(c,hearts(),w*.78f,h*.16f,w*.04f,0xffff6175,true);
      float top=h*.22f,bottom=h*.79f,lw=w*.27f,cx=w/2;
      for(int i=0;i<3;i++){float x=cx+(i-1)*lw;p.setColor(0x2238bcd4);c.drawRoundRect(x-lw*.43f,top,x+lw*.43f,bottom,20,20,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0x665bd9e8);c.drawRoundRect(x-lw*.43f,top,x+lw*.43f,bottom,20,20,p);p.setStyle(Paint.Style.FILL);}
      long now=System.currentTimeMillis();float dt=Math.min(.05f,(now-last)/1000f);last=now;ticks++;
      if(ticks%Math.max(12,46-level/25)==0)drops.add(new Drop(rnd.nextInt(3),top,rnd.nextInt(100)<Math.min(42,13+level/30)));
      Iterator<Drop> it=drops.iterator();while(it.hasNext()){Drop d=it.next();d.y+=h*(.32f+level*.0008f)*dt;float x=cx+(d.lane-1)*lw;if(d.rock){p.setColor(0xff778590);c.drawCircle(x,d.y,w*.033f,p);p.setColor(0xffc9d2d8);c.drawCircle(x-w*.009f,d.y-w*.01f,w*.009f,p);}else{p.setShadowLayer(16,0,0,gold);p.setShader(new RadialGradient(x-w*.01f,d.y-w*.01f,w*.04f,new int[]{0xfffff9b0,0xffffbd19,0xfff47700},null,Shader.TileMode.CLAMP));c.drawCircle(x,d.y,w*.034f,p);p.setShader(null);p.clearShadowLayer();}
        if(d.y>bottom){if(d.lane==lane){if(d.rock){lives--;sfx(ToneGenerator.TONE_PROP_NACK);}else{score+=10;coins++;if(score%50==0)sfx(ToneGenerator.TONE_PROP_ACK);}}it.remove();}}
      if(lives<=0){screen=6;sfx(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD);}else if(score>=100+level*5){if(level<999)level++;save();screen=6;sfx(ToneGenerator.TONE_PROP_PROMPT);}
      float px=cx+(lane-1)*lw,py=bottom+h*.05f;p.setShadowLayer(18,0,0,gold);p.setShader(new LinearGradient(px-w*.04f,py-h*.04f,px+w*.04f,py,0xfffff3a0,0xffff8b00,Shader.TileMode.CLAMP));Path pot=new Path();pot.moveTo(px-w*.045f,py-h*.04f);pot.lineTo(px+w*.045f,py-h*.04f);pot.lineTo(px+w*.035f,py+h*.005f);pot.quadTo(px,py+h*.03f,px-w*.035f,py+h*.005f);pot.close();c.drawPath(pot,p);p.clearShadowLayer();p.setShader(null);p.setColor(gold);c.drawOval(px-w*.05f,py-h*.045f,px+w*.05f,py-h*.025f,p);
      txt(c,"XAL  "+score+" / "+(100+level*5),w/2,h*.90f,w*.037f,cream,true);txt(c,"TOXUN • TOPLA • SƏVİYYƏNİ KEÇ",w/2,h*.945f,w*.026f,blue,false);btn(c,"Ⅱ",w*.12f,h*.07f,w*.12f,h*.05f,false);postInvalidateDelayed(16);
    }
    @Override public boolean onTouchEvent(MotionEvent e){float w=getWidth(),h=getHeight();if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float x=e.getX(),y=e.getY();
      if(screen==0){if(y<h*.57f)start();else if(y<h*.66f)screen=2;else if(y<h*.74f)screen=3;else if(y<h*.83f)screen=4;invalidate();return true;}
      if(screen==2){if(y>h*.80f)screen=0;else if(y>h*.29f){int col=Math.max(0,Math.min(2,(int)(x/w*3)));int row=Math.max(0,Math.min(3,(int)((y/h-.30f)/.105f)));level=Math.max(1,Math.min(999,level-5+row*3+col));save();start();}invalidate();return true;}
      if(screen==3){if(y>h*.83f)screen=0;else if(y<h*.48f)sound=!sound;else if(y<h*.70f)music=!music;else theme=(theme+1)%5;save();invalidate();return true;}
      if(screen==4){screen=0;invalidate();return true;}if(screen==5){if(y>h*.57f)screen=0;else screen=1;invalidate();return true;}
      if(screen==6){if(y>h*.63f)screen=0;else start();invalidate();return true;}
      if(screen==1){if(y<h*.13f&&x<w*.25f){screen=5;invalidate();return true;}float dx=x-downX;if(Math.abs(dx)>w*.06f)lane=Math.max(0,Math.min(2,lane+(dx>0?1:-1)));else if(x<w*.45f)lane=Math.max(0,lane-1);else if(x>w*.55f)lane=Math.min(2,lane+1);invalidate();return true;}return true;}return true;}
  }
}