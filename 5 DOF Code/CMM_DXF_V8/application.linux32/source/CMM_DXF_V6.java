import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import peasy.*; 
import processing.pdf.*; 
import processing.dxf.*; 
import processing.serial.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class CMM_DXF_V6 extends PApplet {


//============================================================
//  3D Digitizer
//  Used with 4 Axis 3D digitizer:
//  Program based off original concept by: http://fablab.ruc.dk/diy-digitizer/
//  Modificaitons and additional features by Bryan Lord Sept 2019
//  DXF and Point Cloud export
//============================================================






float W0X = 0;
float W0Y = 0;
float W0Z = 0;
float Zheight = 52.18f;
PrintWriter output;
boolean W0 = false;
boolean record = false;
boolean projection = false;
int cirSize = 5;
int faceColor=color(0xffC9C7C7);
RawDXF dxf;

//============================================================
//-3D navigation
//============================================================
PeasyCam cam;

//============================================================
//-Digitizer
//============================================================
pointer digitizer = new pointer(this, 0);

//============================================================
//  GUI controls
//============================================================
ArrayList<control> controls = new ArrayList<control>();
control ctl_quit = new control(10, 10, 'q', "  Quit", "Quit program");
control ctl_clear = new control(10, 10+1*40, 'n', "  New", "New - Clear all");
control ctl_point = new control(10, 10+2*40, 'p', " Point", "Mark a point");
control ctl_circle = new control(10, 10+3*40, 'c', " Circle", "Mark a hole");
control ctl_feature = new control(10, 10+4*40, 'f', "Feature", "Start a feature." + "\n" + "Use \"Modify\" to add points");
control ctl_modify = new control(10, 10+5*40, 'm', "Modify", "Modify last figure");
control ctl_dxf = new control(10, 10+6*40, 'z', "  .DXF", "Export 3D DXF file");
control ctl_PtCld = new control(10, 10+7*40, 'x', " PtCld", "Export Point Cloud");
control ctl_pdf = new control(10, 10+8*40, 'd', "  .PDF", "Output PDF" + "\n" + "(Projects to flat surface)");
control ctl_W0 = new control(10, 10+9*40, 'w', "   W0", "Work Coord" + "\n" + "Sets Work Coordinate zero");

//============================================================
//  Objects
//============================================================
ArrayList<object> objects = new ArrayList<object>();

//============================================================
//  Setup
//============================================================
public void setup()
{
  
  output = createWriter("PntCld.txt");
  //Set up visualizer
  cam = new PeasyCam(this, 300);
  cam.setMinimumDistance(50);
  cam.setMaximumDistance(1000);
  //Add GUI controls to display list
  controls.add(ctl_quit);
  controls.add(ctl_clear);
  controls.add(ctl_point); 
  controls.add(ctl_circle); 
  controls.add(ctl_feature);
  controls.add(ctl_modify);
  controls.add(ctl_dxf);
  controls.add(ctl_PtCld);
  controls.add(ctl_pdf);
  controls.add(ctl_W0);

  background(50);
}

//============================================================
//  Handy globals
//============================================================
float penX=0;
float penY=0;
float penZ=0;
float penA=0;
//PVector pen;

object currentObject=null; //-Last created object

//============================================================
//  Draw
//======================================================proce======

public void draw()
{

  //-Get coordinated from digitizer
  digitizer.update();      //-Update globals
  background(0);

  scale(1, -1);            //Reverses the Y coordinate system
  translate(-100, -100);    // Moves the background into the window

  PVector pd=new PVector(digitizer.tip.x, digitizer.tip.y);
  penA=digitizer.rotation;

  pd.rotate(-penA);

  penX=pd.x;
  penY=pd.y;
  penZ=digitizer.tip.z;
  //  pen=digitizer.tip;

  // *********** DXF recording ***************
  if (record == true) {
    String d="DXF_"+Integer.toString(year())+"_"+Integer.toString(day())+"_"+Integer.toString(hour())+"_"+Integer.toString(minute())+"_"+Integer.toString(second())+".dxf";
    beginRaw(DXF, d); 
    scale(1, -1);
    translate(-100, -100);
  }

  //-Draw pad
  stroke(250);
  //  fill(10,10,10,128);
  noFill();
  rect(-40, -28, 305, 235);

  // Draw Tool Home Location
  stroke(100, 500, 0);
  line(248, 254, -Zheight, 258, 254, -Zheight);  // Horizontal Line
  line(248, 264, -Zheight, 248, 254, -Zheight);  // Vertical Line


  //-Draw Orign Triad
  if (W0==false)
  {
    //-Draw Orign Triad
    stroke(255, 0, 0);  // X Axis Line Colour
    line(0, 0, 10, 0);
    stroke(0, 255, 0);  // Y Axis Line Colour
    line(0, 0, 0, 10);
    stroke(0, 0, 255);  // Z Axis Line Colour
    line(0, 0, 0, 0, 0, 10);
  } else
  {
    //-Draw W0 Triad
    stroke(255, 0, 0);  // X Axis Line Colour
    line(W0X, W0Y, W0Z, W0X+10, W0Y, W0Z);
    stroke(0, 255, 0);  // Y Axis Line Colour
    line(W0X, W0Y, W0Z, W0X, W0Y+10, W0Z);
    stroke(0, 0, 255);  // Z Axis Line Colour
    line(W0X, W0Y, W0Z, W0X, W0Y, W0Z+10);
  }

  rotateZ(penA);  // Used for turntable?


  //-Draw all objects
  for (object obj : objects)
  {
    obj.draw();
  }

  if (record == true) {
    endRaw();
    record = false; // Stop recording to the file
  }


  //-Draw cursor
  //  pushMatrix();
  rotateZ(-penA);
  translate(digitizer.tip.x, digitizer.tip.y, digitizer.tip.z);
  stroke(0, 255, 0);
  noFill();
  box(1);
  //  popMatrix();


  //-Draw Heads Up Display
  cam.beginHUD();  
  int faceColor=color(0xffC9C7C7);
  int textColor=color(75, 42, 0);
  stroke(80);
  fill(faceColor);
  rect(width-115, 10, 105, 110);  // XYZ HUD box
    textSize(15);
  
  //-Coordinates
  float fx = (penX);          // Limits the displayed coordinates to 2 dec/pt.
  String sx = nfc(fx, 2);
  float fy = (penY);
  String sy = nfc(fy, 2);
  float fz = (penZ);
  String sz = nfc(fz, 2);
  fill(0); // Text Colour
  text("Mach Coord", width-105, 25);
  text("X: "+ sx, width-100, 45);
  text("Y: "+ sy, width-100, 65);
  text("Z: "+ sz, width-100, 85);
  fill(0); // Text Colour
  text("-----------", width-110, 100);
  //text("R: "+penA, width-110, 90);
  text("Circle: "+ cirSize + "mm", width-110, 114);

  if (W0==true)
  {
    fill(faceColor);
    rect(width-210, 10, 95, 82);  // W0 XYZ HUD box
    float Wfx = (penX - W0X);          // Limits the displayed coordinates to 2 dec/pt.
    String Wsx = nfc(Wfx, 2);
    float Wfy = (penY - W0Y);
    String Wsy = nfc(Wfy, 2);
    float Wfz = (penZ - W0Z);
    String Wsz = nfc(Wfz, 2);
    fill(0); // Text Colour
    text("Work Offset", width-205, 25);
    text("X: "+ Wsx, width-195, 45);
    text("Y: "+ Wsy, width-195, 65);
    text("Z: "+ Wsz, width-195, 85);
  }
  pushStyle();

  //-Update GUI controls 
  if (ctl_clear.update())
  {
    objects.clear();
    W0X = 0;
    W0Y = 0;
    W0Z = 0;
  }
  if (ctl_point.update())
  {
    currentObject=new gpoint(penX, penY, penZ);
    objects.add(currentObject);
    point(penX, penY, penZ);
    output.println(penX + "\t" + penY + "\t" + penZ);
  }
  if (ctl_circle.update())
  {
    currentObject=new gcircle(penX, penY, penZ);
    objects.add(currentObject);
  }
  if (ctl_feature.update())
  {
    currentObject=new gfeature(penX, penY, penZ);
    objects.add(currentObject);
  }
  if (ctl_modify.update())
  {
    if (currentObject!=null)
    {
      currentObject.modify(new PVector(penX, penY, penZ));
    }
  }
  
  // ***********    DXF File Save
  if (ctl_dxf.update())
  {
    record = true; // DXF record switch
  }

  // ***********    PointCloud File Save
  if (ctl_PtCld.update())
  {
    output.flush(); // Writes the remaining data to the file
    output.close(); // Finishes the file
  }

  // ***********    PDF File Save
  if (ctl_pdf.update())
  {
    String s="PDF_"+Integer.toString(year())+"_"+Integer.toString(day())+"_"+Integer.toString(hour())+"_"+Integer.toString(minute())+"_"+Integer.toString(second())+".pdf";
    beginRecord(PDF, s); 
    scale(1, -1);
    translate(400, -500);
    background(255);
    stroke(0);
    fill(0); 
    rect(-380, -250, 10, 10);
    scale(1, -1);
    fill(0); // Text Colour
    text("10mm", -390, 230);
    scale(1, -1);

    for (object obj : objects)
    {
      obj.project();
    }

    for (object obj : objects)
    {
      obj.export();
    }
    endRecord();
  }

  // W0 Zero
  if (ctl_W0.update())
  {
    W0X=penX;
    W0Y=penY;
    W0Z=penZ;
    W0 = true;
  }




  // *********** Exit Program
  if (ctl_quit.update())
  {
    exit(); // Stops the program
  }


  //-Draw GUI controls
  for (control ctl : controls)
  {
    if (ctl.change)
      ctl.draw();
    if (ctl.mouseOver)
      ctl.drawHelp();
  }
  popStyle();
  cam.endHUD();
}
//============================================================
//  Simple GUI controls
//  Controls are activated by key and mouse click
//  Contextual help is displayed on mouse over
//  (F)DZL 2015
//============================================================

class control
{
  //Position and size of HUD Menu windows (individual Sizes)
  int x=0;
  int y=0;
  int w=60;
  int h=35;
  //-Colors
  int faceColor=color(98, 206, 198);
  int textColor=color(75, 42, 0);
  //-Key to activate control
  char hotkey;
  //-Discriptive caption
  String caption;
  //-Mouse-over help 
  String help;
  //-Mouse and key state
  boolean clickState=false;
  boolean keyState=false;
  boolean mouseOver=false;
  //-Updated for redraw
  boolean change=false;
  //-Typematic control
  int timer=0;
  boolean typematic=false;


  //-Constructor
  control(int px, int py, char k, String c, String h)
  {
    x=px;
    y=py;
    hotkey=k;
    caption=c;
    help=h;
  }


  //-Draw
  public void draw()
  {
    if (clickState||keyState)
      stroke(255, 0, 0);
    else
    stroke(100);
    fill(0xffC9C7C7);  // background colour of Menu
    rect(x, y, w, h);  // draws the rectangle
    fill(textColor);
    textSize(20);
    text(" -"+hotkey+"-", x+5, y+17);  // First line of text
    textSize(14);
    text(caption, x+6, y+33);  // Second line of text
  }


  //-Draw help
  public void drawHelp()
  {
    textSize(20);
    fill(200);
    text(help, x+w+5, y+20);
  }


  //-Update. Handles keys and mouse
  public boolean update()
  {
    boolean result=false;
    mouseOver=false;
    if ((mouseX>x)&&(mouseX<(x+w)) && (mouseY>y)&&(mouseY<(y+h)))
    {
      mouseOver=true;
      if (mousePressed==true)
      {
        if (!clickState)
        {
          clickState=true;
          change=true;
        }
      } else
      {
        if (clickState==true)
        {
          result=true;
          clickState=false;
          change=true;
        }
      }
    } else
      if (mousePressed==false)
      {      
        clickState=false;
        change=true;
      }     


    if (key==hotkey)
    {
      if (keyPressed==true)
      {
        mouseOver=true;

        if (!keyState)
        {
          keyState=true;
          change=true;
          timer=millis()+500;
          result=true;
        } else
        {
          if (typematic)
          {
            if (millis()>timer)
            {
              timer+=10;
              result=true;
            }
          }
        }
      } else
      {
        if (keyState==true)
        {
          keyState=false;
          change=true;
        }
      }
    } else
      if (keyPressed==false)
      {
        keyState=false;
        change=true;
      }
    return result;
  }
}

public void keyPressed() {
  if (key == '+') {
    cirSize=cirSize+1;
  }
  if (key == '-') {
    cirSize=cirSize-1;
  }
}
//============================================================
//  Measuring objects for 3D digitizer
//  Objects implements function for 3D and 2D screen drawing and
//  2D export.
//  (F)DZL 2015   
//============================================================
//============================================================
// Base class for all objects
//============================================================
public abstract class object
{
  public abstract void draw();  //-Draw to screen (3D)
  public abstract void modify(PVector p); //-Modify object
  public abstract void project(); //-Draw to 2D
  public abstract void export(); //-Export (draw .PDF compatible)

};
//============================================================
// Single 3D point (1mmm box)
//============================================================
class gpoint extends object
{
  PVector pos;
  int lineColor=color(255);
  int projectColor=color(100);
  int exportColor=color(0);
  gpoint(PVector p)
  {
    pos.x=p.x;
    pos.y=p.y;
    pos.z=p.z;
  }
  gpoint(float x, float y, float z)
  {
    pos=new PVector(x, y, z);
  }
  public void draw()
  {
    stroke(lineColor);
    pushMatrix();
    translate(pos.x, pos.y, pos.z);
    box(1);
    popMatrix();
  }

  public void project()
  {
    stroke(projectColor);
    line(pos.x-5, pos.y, pos.x+5, pos.y);
    line(pos.x, pos.y-5, pos.x, pos.y+5);
  }

  public void export()
  {
    stroke(exportColor);
    line(pos.x-5, pos.y, pos.x+5, pos.y);
    line(pos.x, pos.y-5, pos.x, pos.y+5);
  }

  public void modify(PVector p)
  {
    pos.x=p.x;
    pos.y=p.y;
    pos.z=p.z;
  }
}

//============================================================
// Single circle 
//============================================================
class gcircle extends object
{
  PVector pos;
  int projectColor=color(100);
  int exportColor=color(0);
  int lineColor=color(255);
  boolean filled=true;
  gcircle(PVector p)
  {
    pos.x=p.x;
    pos.y=p.y;
    pos.z=p.z;
  }
  gcircle(float x, float y, float z)
  {
    pos=new PVector(x, y, z);
  }
  public void draw()
  {
    stroke(lineColor);
    pushMatrix();
    translate(pos.x, pos.y, pos.z);
    ellipse(0, 0, cirSize, cirSize);
    popMatrix();
  }
  public void project()
  {
    stroke(projectColor);
    ellipse(pos.x, pos.y, cirSize, cirSize);
  }

  public void export()
  {
    if (filled)
    {
      noStroke();
      fill(exportColor);
    } else
    {
      noFill();
      stroke(exportColor);
    }
    ellipse(pos.x, pos.y, 10, 10);
  }

  public void modify(PVector p)
  {
    pos.x=p.x;
    pos.y=p.y;
    pos.z=p.z;
  }
}

//============================================================
//  Open loop feature
//============================================================
class gfeature extends object
{
  boolean filled=true;
  PVector pos=new PVector(0, 0, 0);
  int projectColor=color(100);
  int exportColor=color(100, 100, 0);
  int lineColor=color(255, 255, 0);
  int anchorColor=color(0, 255, 0);
  ArrayList<PVector> figure = new ArrayList<PVector>();
  gfeature(PVector p)
  {
    pos.x=p.x;
    pos.y=p.y;
    pos.z=p.z;
    figure.add(new PVector(pos.x, pos.y, pos.z));
  }
  gfeature(float x, float y, float z)
  {
    pos=new PVector(x, y, z);
    figure.add(new PVector(pos.x, pos.y, pos.z));
  }

  public void draw()
  {
    pushMatrix();
    translate(pos.x, pos.y, pos.z);
    stroke(anchorColor);
    ellipse(0, 0, 5, 5);
    popMatrix();
    float x0=pos.x;
    float y0=pos.y;
    float z0=pos.z;

    stroke(lineColor);

    for (PVector p : figure)
    {
      line(x0, y0, z0, p.x, p.y, p.z);
      x0=p.x;
      y0=p.y;
      z0=p.z;
    }
    line(x0, y0, z0, pos.x, pos.y, pos.z);
  }

  public void project()
  {
    stroke(projectColor);
    float x0=pos.x;
    float y0=pos.y;
    float z0=pos.z;
    for (PVector p : figure)
    {
      line(x0, y0, p.x, p.y);
      x0=p.x;
      y0=p.y;
      z0=p.z;
    }
    line(x0, y0, pos.x, pos.y);
  }

  public void export()
  {
    if (filled)
    {
      noStroke();
      fill(exportColor);
    } else
    {
      noFill();
      stroke(exportColor);
    }

    PShape loop=createShape();
    loop.beginShape();

    for (PVector p : figure)
    {
      loop.vertex(p.x, p.y);
    }
    loop.vertex(pos.x, pos.y);
    loop.endShape();
    shape(loop);
  }

  public void modify(PVector p)
  {
    figure.add(new PVector(p.x, p.y, p.z));
  }
}


Serial myPort;  // Create object from Serial class

float PX=0;
float PY=0;
float PZ=0;
float ROT=0;

float sx=0;
float sy=0;
float sz=0;
float sa=0;

char datatype;
byte [] inData = new byte[5];
int inptr=0;
int rstate=0;

float www=0;
static final int zero = 0;
static final int one = 1;


public void serialEvent(Serial myPort) 
{
  while (myPort.available ()>0)
  {
    switch(rstate)
    {
    case zero:
      datatype = myPort.readChar();
      if (datatype=='x'||datatype=='y'||datatype=='z'||datatype=='a')
      {
        rstate=1;
        inptr=0;
      }
      break;

    case one:
      inData[inptr++]=(byte)myPort.readChar();
      if (inptr==4)
      {
        int intbit = 0;
        intbit = (inData[3] << 24) | ((inData[2] & 0xff) << 16) | ((inData[1] & 0xff) << 8) | (inData[0] & 0xff);
        float f = Float.intBitsToFloat(intbit);

        switch(datatype)
        {
        case 'x': 
          sx=f;
          break;
        case 'y': 
          sy=f;
          break;
        case 'z': 
          PX=sx;
          PY=sy;
          PZ=f;
          ROT=sa;
         
          break;
        case 'a':
          sa=f;
          break;
        }
        rstate=0;
      }        
      break;
    }
  }
}


class pointer
{
  PVector tip = new PVector(0, 0, 0);
  float rotation=0;
  boolean active=false;
  boolean demoMode=false;        // = true for offline testing
  pointer(PApplet p,int index)
  {
    myPort =new Serial(p, Serial.list()[2], 19200); // Comment out for offline testing
    active=true;

  }

  public void update()
  {
    if (demoMode)
    {
     // tip.x=(mouseX/2)-200;
     // tip.y=(mouseY/-2)+300;
     // tip.z=0;
    }
    else
    {
//      tip.set(PX,PY,PZ);
      tip.x=PX;
      tip.y=PY; 
      tip.z=PZ-Zheight; // Add to this value to move the pointer starting positon
      rotation=ROT;
    }
  }
}
  public void settings() {  size(1200, 800, P3D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "CMM_DXF_V6" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
