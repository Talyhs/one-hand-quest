package com.araz.onehand;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(0xff0c1122);
    getWindow().setNavigationBarColor(0xff0c1122);
    setContentView(new Game(this));
  }
  static class Orb { int lane; float y; boolean bad; Orb(int l,float y,boolean b){lane=l;this.y=y;bad=b;} }
  class Game extends View {
    Paint p=new Paint(3); Random rnd=new Random(); SharedPreferences pref;
    ArrayList<Orb> orbs=new ArrayList<>();
    int level,coins,score,lives,lane,ticks; boolean started,over,won;
    float downX; long last=System.currentTimeMillis();
    int[] colors={0xff22d3ee,0xffa78bfa,0xfff472b6,0xfffbbf24,0xff4ade80};
    Game(Context c){super(c);pref=getSharedPreferences("game",0);level=pref.getInt("level",1);coins=pref.getInt("coins",0);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void save(){pref.edit().putInt("level",level).putInt("coins",coins).apply();}
    void reset(){score=0;lives=3;lane=1;ticks=0;orbs.clear();started=true;over=false;won=false;last=System.currentTimeMillis();invalidate();}
    @Override protected void onDraw(Canvas c){
      float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
      p.setShader(new LinearGradient(0,0,w,h,0xff10172d,0xff251642,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
      p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-black",0));p.setColor(-1);p.setTextSize(w*.064f);c.drawText("QARA QIZIL",w/2,h*.075f,p);
      p.setTypeface(Typeface.DEFAULT);p.setTextSize(w*.035f);p.setColor(0xffbac7e8);c.drawText("SƏVİYYƏ "+level+" / 999",w/2,h*.115f,p);
      p.setTextAlign(Paint.Align.LEFT);p.setColor(0xffffd166);p.setTextSize(w*.04f);c.drawText("◆ "+coins,w*.07f,h*.16f,p);
      p.setTextAlign(Paint.Align.RIGHT);p.setColor(0xffff6688);c.drawText("♥ ".repeat(Math.max(0,lives)),w*.93f,h*.16f,p);
      float top=h*.22f,bottom=h*.78f,lw=w*.23f,cx=w/2;
      for(int i=0;i<3;i++){float x=cx+(i-1)*lw;p.setColor(0x2235d8ed);c.drawRoundRect(x-lw*.42f,top,x+lw*.42f,bottom,22,22,p);p.setStyle(Paint.Style.STROKE);p.setColor(0x8852e5f2);c.drawRoundRect(x-lw*.42f,top,x+lw*.42f,bottom,22,22,p);p.setStyle(Paint.Style.FILL);}
      if(started&&!over&&!won){
        long now=System.currentTimeMillis();float dt=Math.min(.05f,(now-last)/1000f);last=now;ticks++;
        if(ticks%Math.max(18,55-level/3)==0)orbs.add(new Orb(rnd.nextInt(3),top,rnd.nextInt(100)<Math.min(45,20+level/5)));
        Iterator<Orb> it=orbs.iterator();
        while(it.hasNext()){Orb o=it.next();o.y+=h*(.38f+level*.003f)*dt;float x=cx+(o.lane-1)*lw;p.setColor(o.bad?0xffff5577:colors[level%colors.length]);p.setShadowLayer(14,0,0,p.getColor());c.drawCircle(x,o.y,w*.035f,p);p.clearShadowLayer();if(o.y>bottom-h*.035f){if(o.lane==lane){if(o.bad){lives--;score=Math.max(0,score-3);}else{score+=10;coins++;}}it.remove();}}
        if(lives<=0)over=true;
        if(score>=100+level*5){won=true;if(level<999)level++;save();}
        postInvalidateDelayed(16);
      } else {for(Orb o:orbs){p.setColor(o.bad?0xffff5577:colors[level%colors.length]);c.drawCircle(cx+(o.lane-1)*lw,o.y,w*.035f,p);}}
      float px=cx+(lane-1)*lw,py=bottom+h*.045f;p.setColor(0xfff8fafc);p.setShadowLayer(18,0,0,0xff22d3ee);
      Path ship=new Path();ship.moveTo(px,py-h*.045f);ship.lineTo(px-w*.035f,py+h*.018f);ship.lineTo(px,py+h*.005f);ship.lineTo(px+w*.035f,py+h*.018f);ship.close();c.drawPath(ship,p);p.clearShadowLayer();p.setTextAlign(Paint.Align.CENTER);
      if(!started||over||won){
        p.setColor(0xdd080d1d);c.drawRoundRect(w*.08f,h*.34f,w*.92f,h*.69f,26,26,p);
        p.setColor(-1);p.setTypeface(Typeface.create("sans-serif-black",0));p.setTextSize(w*.07f);c.drawText(!started?"READY?":won?"SƏVİYYƏ COMPLETE":"GAME OVER",w/2,h*.42f,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(w*.04f);p.setColor(0xffc6d0ed);
        if(!started){c.drawText("Toxun və ya sürüşdür",w/2,h*.50f,p);c.drawText("Qızılı topla, qırmızıdan yayın",w/2,h*.55f,p);c.drawText("999 səviyyəni keç!",w/2,h*.59f,p);}
        else {c.drawText("Score: "+score+"  Coins: "+coins,w/2,h*.52f,p);c.drawText("Tap to continue",w/2,h*.58f,p);}
        p.setColor(0xff22d3ee);c.drawRoundRect(w*.28f,h*.61f,w*.72f,h*.66f,16,16,p);p.setColor(0xff081225);p.setTypeface(Typeface.create("sans-serif-black",0));p.setTextSize(w*.04f);c.drawText(!started?"PLAY":won?"NEXT LEVEL":"PLAY AGAIN",w/2,h*.642f,p);
      } else {p.setColor(0xffdbeafe);p.setTextSize(w*.035f);p.setTypeface(Typeface.DEFAULT);c.drawText("SCORE "+score+" / "+(100+level*5),w/2,h*.89f,p);p.setColor(0xff8b9ac4);c.drawText("Tap sides or swipe to move",w/2,h*.93f,p);}
    }
    @Override public boolean onTouchEvent(MotionEvent e){
      float w=getWidth();
      if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return true;}
      if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX;if(!started||over||won){if(won&&level>=999)level=1;reset();return true;}if(Math.abs(dx)>w*.08f)lane=Math.max(0,Math.min(2,lane+(dx>0?1:-1)));else if(e.getX()<w*.45f)lane=Math.max(0,lane-1);else if(e.getX()>w*.55f)lane=Math.min(2,lane+1);invalidate();return true;}
      return true;
    }
  }
}