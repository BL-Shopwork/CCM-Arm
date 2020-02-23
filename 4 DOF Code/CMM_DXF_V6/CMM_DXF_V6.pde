
//============================================================
//  3D Digitizer
//  Used with 4 Axis 3D digitizer:
//  Program based off original concept by: http://fablab.ruc.dk/diy-digitizer/
//  Modificaitons and additional features by Bryan Lord Sept 2019
//  DXF and Point Cloud export
//============================================================

import processing.serial.*;
import peasy.*;
import processing.pdf.*;
import processing.dxf.*;

float W0X = 0;
float W0Y = 0;
float W0Z = 0;
float Zheight = 52.18;
PrintWriter output;
boolean W0 = false;
boolean record = false;
boolean projection = false;
int cirSize = 5;
color faceColor=color(#C9C7C7);
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
void setup()
{
  size(1200, 800, P3D);
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

void draw()
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
  color faceColor=color(#C9C7C7);
  color textColor=color(75, 42, 0);
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
