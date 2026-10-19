package com.araz.onehand;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(5, 12, 19));
    getWindow().setNavigationBarColor(Color.rgb(5, 12, 19));
    setContentView(new OilGame(this));
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
    final ArrayList<Drop> drops = new ArrayList<>();
    final SharedPreferences prefs;
    final int gold = Color.rgb(255,190,62);
    final int cream = Color.rgb(255,235,177);
    float bucketX, bucketY, fingerX, lastX;
    int score=0, lives=3, level, coins, best, ticks=0;
    boolean started=false, gameOver=false, levelWon=false;
    long lastFrame=System.currentTimeMillis();
    OilGame(Context context) {
      super(context);
      prefs=getSharedPreferences("black_gold_save", MODE_PRIVATE);
      level=Math.max(1, Math.min(120, prefs.getInt("level",1)));
      coins=prefs.getInt("coins",0);
      best=prefs.getInt("best",0);
      setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    void save() {
      prefs.edit().putInt("level",level).putInt("coins",coins).putInt("best",best).apply();
    }
    void startGame() {
      score=0; lives=3; ticks=0; drops.clear(); started=true; gameOver=false; levelWon=false;
      lastFrame=System.currentTimeMillis(); invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      float w=getWidth(), h=getHeight();
      if(w<=0 || h<=0) return;
      bucketY=h*.81f;
      drawBackground(canvas,w,h);
      drawOilRig(canvas,w,h);
      if(started && !gameOver && !levelWon) {
        long now=System.currentTimeMillis();
        float dt=Math.min(.045f,Math.max(.008f,(now-lastFrame)/1000f));
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
            if(d.stone) { lives--; score=Math.max(0,score-5); }
            else { score+=10; coins++; best=Math.max(best,score); }
            it.remove();
          } else if(d.y>h+d.r) {
            if(!d.stone) lives--;
            it.remove();
          }
        }
        if(score>=levelGoal()) {
          levelWon=true;
          if(level<120) level++;
          save();
        }
        if(lives<=0) { gameOver=true; save(); }
        postInvalidateDelayed(16);
      } else {
        for(Drop d:drops) drawDrop(canvas,d);
      }
      drawHUD(canvas,w,h);
      drawBucket(canvas,w,h);
      if(!started || gameOver || levelWon) drawPanel(canvas,w,h);
      else {
        p.setColor(0xffd5e1e9); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.032f);
        canvas.drawText("Qabı barmağınla sağa-sola sürüşdür",w/2,h*.94f,p);
      }
    }
    int fiftyFive(){ return 55; }
    int levelGoal(){ return 100 + (level-1)*15; }
    void drawBackground(Canvas c,float w,float h) {
      p.setShader(new LinearGradient(0,0,0,h,0xff07111c,0xff163448,Shader.TileMode.CLAMP));
      c.drawRect(0,0,w,h,p); p.setShader(null);
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
      }
    }
    void drawHUD(Canvas c,float w,float h) {
      p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL));
      p.setColor(cream); p.setTextSize(w*.065f); c.drawText("QARA QIZIL",w/2,h*.065f,p);
      p.setTypeface(Typeface.DEFAULT_BOLD); p.setColor(0xffb9ccd7); p.setTextSize(w*.031f); c.drawText("DAMCI OVÇUSU  •  SƏVİYYƏ "+level+"/120",w/2,h*.103f,p);
      p.setTextAlign(Paint.Align.LEFT); p.setColor(gold); p.setTextSize(w*.041f); c.drawText("XAL  "+score,w*.055f,h*.155f,p);
      p.setTextAlign(Paint.Align.RIGHT); p.setColor(0xffff7a76); c.drawText("♥ ".repeat(Math.max(0,lives)),w*.945f,h*.155f,p);
      p.setTextAlign(Paint.Align.CENTER); p.setColor(0xffc5d7e2); p.setTextSize(w*.029f); c.drawText("Sikkə: "+coins+"     Rekord: "+best,w/2,h*.19f,p);
    }
    void drawBucket(Canvas c,float w,float h) {
      if(bucketX==0) bucketX=w/2;
      float bw=w*.19f, bh=h*.055f, x=bucketX, y=bucketY;
      p.setShadowLayer(14,0,0,0xffe6a52c); p.setColor(0xffd49a38);
      Path b=new Path(); b.moveTo(x-bw/2,y-bh/2); b.lineTo(x+bw/2,y-bh/2); b.lineTo(x+bw*.36f,y+bh/2); b.quadTo(x,y+bh*.75f,x-bw*.36f,y+bh/2); b.close(); c.drawPath(b,p); p.clearShadowLayer();
      p.setColor(cream); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(3,w*.008f)); c.drawLine(x-bw*.56f,y-bh*.52f,x+bw*.56f,y-bh*.52f,p); p.setStyle(Paint.Style.FILL);
      p.setColor(0xff70400b); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(w*.022f); c.drawText("NEFT",x,y+bh*.20f,p);
    }
    void drawPanel(Canvas c,float w,float h) {
      p.setColor(0xe8091722); c.drawRoundRect(w*.075f,h*.29f,w*.925f,h*.69f,28,28,p);
      p.setColor(0x99f5b73b); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); c.drawRoundRect(w*.075f,h*.29f,w*.925f,h*.69f,28,28,p); p.setStyle(Paint.Style.FILL);
      p.setTextAlign(Paint.Align.CENTER); p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL)); p.setColor(gold); p.setTextSize(w*.075f);
      c.drawText(!started?"QARA QIZIL":levelWon?"SƏVİYYƏ KEÇİLDİ":"OYUN BİTDİ",w/2,h*.39f,p);
      p.setTypeface(Typeface.DEFAULT); p.setColor(0xffe4edf2); p.setTextSize(w*.039f);
      if(!started) {
        c.drawText("Göydən düşən neft damcılarını tut!",w/2,h*.46f,p);
        c.drawText("Boz daşlardan yayın, qabını hərəkət etdir.",w/2,h*.505f,p);
        c.drawText("Hər qızılı damcı +10 xal verir.",w/2,h*.55f,p);
      } else if(levelWon) {
        c.drawText("Əla! Növbəti səviyyə: "+level,w/2,h*.47f,p);
        c.drawText("Topladığın xallar: "+score,w/2,h*.52f,p);
      } else {
        c.drawText("Topladığın xal: "+score,w/2,h*.47f,p);
        c.drawText("Rekord: "+best+"   •   Sikkələr: "+coins,w/2,h*.52f,p);
        c.drawText("Bir daha sına!",w/2,h*.565f,p);
      }
      p.setColor(gold); c.drawRoundRect(w*.27f,h*.595f,w*.73f,h*.655f,18,18,p);
      p.setColor(0xff1b160b); p.setTypeface(Typeface.create("sans-serif-black",Typeface.NORMAL)); p.setTextSize(w*.042f);
      c.drawText(!started?"OYUNA BAŞLA":levelWon?"DAVAM ET":"YENİDƏN OYNA",w/2,h*.635f,p);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
      float w=getWidth();
      if(e.getAction()==MotionEvent.ACTION_DOWN) { fingerX=e.getX(); lastX=fingerX; return true; }
      if(e.getAction()==MotionEvent.ACTION_MOVE) {
        if(started && !gameOver && !levelWon) bucketX=Math.max(w*.11f,Math.min(w*.89f,e.getX()));
        invalidate(); return true;
      }
      if(e.getAction()==MotionEvent.ACTION_UP) {
        if(!started || gameOver || levelWon) {
          if(level>=120 && levelWon) level=1;
          startGame(); return true;
        }
        bucketX=Math.max(w*.11f,Math.min(w*.89f,e.getX())); invalidate(); return true;
      }
      return true;
    }
  }
}