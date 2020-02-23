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

// DH Parameters of each link point
float DHx1=0;
float DHy1=0;
float DHz1=0;
float DHx2=0;
float DHy2=0;
float DHz2=0;
float DHx3=0;
float DHy3=0;
float DHz3=0;
float DHx4=0;
float DHy4=0;
float DHz4=0;

float XOff=0;
float YOff=0;
float ZOff=0;

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
      if (datatype=='x'||datatype=='y'||datatype=='z'||datatype=='i'||datatype=='o'||datatype=='v'||datatype=='j'||datatype=='k'||datatype=='l'||datatype=='b'||datatype=='g'||datatype=='m'||datatype=='I'||datatype=='O'||datatype=='P'||datatype=='E'||datatype=='R'||datatype=='T')
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
          break;

        case 'i': 
          DHx1=f;
          break;
        case 'o': 
          DHy1=f;
          break; 
        case 'v': 
          DHz1=f;
          break;
        case 'j': 
          DHx2=f;
          break;
        case 'k': 
          DHy2=f;
          break; 
        case 'l': 
          DHz2=f;
          break;
        case 'b': 
          DHx3=f;
          break;
        case 'g': 
          DHy3=f;
          break; 
        case 'm': 
          DHz3=f;
          break;
        case 'I': 
          DHx4=f;
          break;
        case 'O': 
          DHy4=f;
          break; 
        case 'P': 
          DHz4=f;
          break; 
        case 'E': 
          XOff=f;
          break;
        case 'R': 
          YOff=f;
          break; 
        case 'T': 
          ZOff=f;
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
  pointer(PApplet p, int index)
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
    } else
    {
      // tip.set(PX,PY,PZ);
      tip.x=PX;
      tip.y=PY; 
      tip.z=PZ-Zheight; // Add to this value to move the pointer starting positon
    }
  }
}
