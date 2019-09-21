//*************************************************
//  3D digitizer
//  See description of project here:
//  Updated for 4 encoder - Bryan Lord 2019
//  Original Concept(F)Dzl 2018
//
//*************************************************

#include "dzl_encoders.h"
#include "math.h"

IQencoder E0, E1, E2, E3;

//*************************************************
//  Mechanical set up:
//*************************************************
#define ARM1 200.7       // Arm E1-R2 length[mm.]
#define ARM2 200.7       // Arm E2-tip length[mm.]
#define ARM3 101.6025      // Will be used for the additional Tip (TO BE DONE)

//Offsets from mechanical set-up:
#define Z_OFFSET 204.02   // E1 axis height above table from Surface Plate (-52 if no plate) [204.02mm to Alum Base Plate.]
#define X_OFFSET 0.557   // Distance from E0 axis to preset position [0mm.] - Offset to fine tune where you want the X Origin
#define Y_OFFSET 249.12   // Distance from E0 axis to preset position [250mm.] - Offset to fine tune where you want the Y Origin

//Angles from mechanical set-up:
#define E0_PRESET 1.25   // based on starting origin located at Top Right Corner (1.25)
#define E1_PRESET 115.52
#define E2_PRESET 84.22
#define E3_PRESET 160.25


//*************************************************
//  Send coordinates to processing program
//*************************************************
void sendFloat(float f, unsigned t) {
  byte * b = (byte *) &f;
  Serial.write(t);
  Serial.write(b[0]);
  Serial.write(b[1]);
  Serial.write(b[2]);
  Serial.write(b[3]);
}

void setup()
{
  Serial.begin(19200);

  setEncoderRate(10000);

  //Attach encoders so anti-clockwise rotation is positive:
  E0.attach(9, 8);      //E0 (W,G)  Pivot
  E1.attach(11, 10);    //E1 (W,G)  Shoulder
  E2.attach(12, 13);    //E2 (G,W)  Elbow
  E3.attach(7, 6);      //tt (W,G)  Wrist

  delay(10);            //-Allow time to settle

  //Preset encoders:
  E0.setDegrees(E0_PRESET);     //Horizontal encoder (corner)
  E1.setDegrees(E1_PRESET);     //First vertical encoder
  E2.setDegrees(E2_PRESET);     //Second vertical encoder
  E3.setDegrees(E3_PRESET);     //Turntable
}


// **************** LOOP **************************
void loop()
{
  //Read encoders in degrees
  double A = E0.getDegrees();
  double B = E1.getDegrees();
  double C = E2.getDegrees();
  double D = E3.getDegrees();

  /*
    //Read encoders in radians
    double A = E1.getRadians();
    double B = E2.getRadians();
    double C = E0.getRadians();
    double D = E3.getRadians();
  */

  double A4 = B - 90;
  double d2 = sin(A4 * (PI / 180)) * ARM1;
  double A5 = 180 - (A4 + 90);
  double A6 = C - A5;
  double d3 = cos(A6 * (PI / 180)) * ARM2;
  double d4 = cos(A4 * (PI / 180)) * ARM1;
  double d5 = sin(A6 * (PI / 180)) * ARM2;
  double A7 = 180 - (A6 + 90);
  double A8 = D - 90 - A7;
  double d6 = cos(A8 * (PI / 180)) * ARM3;
  double d7 = sin(A8 * (PI / 180)) * ARM3;
  double z = Z_OFFSET + d2 - d3 - d6;
  double r = d4 + d5 + d7;


  double x = r * cos(A * (PI / 180)) + X_OFFSET;
  double y = r * sin(A * (PI / 180)) + Y_OFFSET;


  //Send coordinates to Processing
  sendFloat(x, 'x');
  sendFloat(y, 'y');
  sendFloat(z, 'z');
  //sendFloat(E, 'a');  // Uncomment to send rotary Table
  
/*
    Serial.print ("X: ");
    Serial.print(x);
    Serial.print(",");
    Serial.print ("Y: ");
    Serial.print(y);
    Serial.print(",");
    Serial.print ("Z: ");
    Serial.println(z);

    //Print encoder angles:
    Serial.print ("E0: ");
    Serial.print(E0.getDegrees());
    Serial.print(",  E1: ");
    Serial.print(E1.getDegrees());
    Serial.print(",  E2: ");
    Serial.print(E2.getDegrees());
    Serial.print(",  E3: ");
    Serial.println(E3.getDegrees());
*/

delay(100);

}
