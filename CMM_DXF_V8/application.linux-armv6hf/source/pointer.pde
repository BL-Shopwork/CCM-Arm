import processing.serial.*;

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


void serialEvent(Serial myPort) 
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

  void update()
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
