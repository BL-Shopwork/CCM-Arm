//*************************************************
//  3D digitizer - 5 DOF using Denavit-Hartenburg parameters
//  Description:
//
//  Bryan Lord 2019
//*************************************************

#include "encoders.h"
#include "math.h"
#include "Geometry.h"

long previousMillis = 0;

IQencoder E0, E1, E2, E3, E4;

//*************************************************
//  Mechanical set up:
//*************************************************
#define ARM1 204       // Arm E0-E1 length[mm.] (Base to E1)
#define ARM2 200.7       // Arm E1-E2 length[mm.]
#define ARM3 238.278       // Arm E2-E4 length[mm.]
#define ARM4 101.6025    // Stylis E4 to End-Effector

//Offsets from mechanical set-up:   ***************** These will likely not be used. Check to see if needed
#define Z_OFFSET -45           // E1 axis height above table from Surface Plate (-52 if no plate) [204.02mm to Alum Base Plate.]
#define X_OFFSET 0           // Distance from E0 axis to preset position [0mm.] - Offset to fine tune where you want the X Origin
#define Y_OFFSET 246         // Distance from E0 axis to preset position [250mm.] - Offset to fine tune where you want the Y Origin

//Angles from mechanical set-up:
#define E0_PRESET 183.5 // 183.75 / -85.95 / 175.95
#define E1_PRESET 41.85 //42.9
#define E2_PRESET -138 // -136.35
#define E3_PRESET 0
#define E4_PRESET 86.25 //87.15

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


//*************************************************
//  Kinematic Chain
//*************************************************

// Link stores the D-H parameters for one link in the chain. It's an abstract base class so to use it you have to subclass it and define the Move function, more on that later though
class Link
{
  public:
    float d, theta, r, alpha;

    Link(float _d, float _theta, float _r, float _alpha) : d(_d), theta(_theta), r(_r), alpha(_alpha) { }
    virtual void Move(float amount) = 0;
};

// KinematicChain manages the links and implements the forward and inverse kinematics
template<int maxLinks> class KinematicChain
{
    // A few variables used in the inverse kinematics defined here to save re-allocating them every time inverse kinematics is called
    Point deltaPose;
    Matrix<3, 1> jacobian;
    Matrix<maxLinks> deltaAngles;
    Transformation currentPose, perturbedPose;

    // The number of links addedto the chain via AddLink
    unsigned int noOfLinks;

  public:
    // An array containing all the D-H parameters for the chain
    Link *chain[maxLinks];

    KinematicChain() {
      noOfLinks = 0;
    }

    // Add a link - it's D-H parameters as well as a function pointer describing it's joint movement
    void AddLink(Link &l)//float d, float theta, float r, float alpha, void (*move)(Link&, float))
    {
      if (noOfLinks == maxLinks)
        return;

      chain[noOfLinks++] = &l;
    }

    int NoOfLinks() {
      return noOfLinks;
    }


    //***********************
    // Transforms pose from the end effector coordinate frame to the base coordinate frame.
    Transformation &ForwardKinematics(Transformation &pose)
    {
      for (int i = noOfLinks - 1; i >= 0; i--)
      {
        // These four operations will convert between two coordinate frames defined by D-H parameters, it's pretty standard stuff
        pose.RotateX(chain[i]->alpha);
        pose.Translate(chain[i]->r, 0, 0);
        pose.Translate(0, 0, chain[i]->d);
        pose.RotateZ(chain[i]->theta);
      }
      return pose;
    }

    // Handy overload to save having to feed in a fresh Transformation every time
    Transformation ForwardKinematics()
    {
      currentPose = Identity<4, 4>();
      return ForwardKinematics(currentPose);
    }
};


// Define a revolute joint which changes the theta D-H parameter when it moves
class RevoluteJoint : public Link
{
  public:
    RevoluteJoint(float d, float theta, float r, float alpha) : Link(d, theta, r, alpha) { }
    void Move(float amount) {
      theta += amount;
    }
};


// Define a prismatic joint which changes the r parameter. We migh also throw in a parameter 'stiffness' to make the joint more reluctant move in the IK
class PrismaticJoint : public Link
{
    float stiffness;
  public:
    PrismaticJoint(float d, float theta, float r, float alpha, float _stiffness = 1) : Link(d, theta, r, alpha), stiffness(_stiffness) { }
    void Move(float amount) {
      r += (amount * stiffness);
    }
};

// Define a joint that doesn't move at all. The IK will effectively ignore this one
class ImmobileJoint : public Link
{
  public:
    ImmobileJoint(float d, float theta, float r, float alpha) : Link(d, theta, r, alpha) { }
    void Move(float amount) { }
};


//*************************************************
//  SETUP
//*************************************************
void setup()
{
  Serial.begin(19200);

  setEncoderRate(10000);

  //Attach encoders to match Kinematic Right Hand Rule:
  E0.attach(9, 8);      //E0 (W,G)  Pivot
  E1.attach(11, 10);    //E1 (W,G)  Shoulder
  E2.attach(12, 13);    //E2 (G,W)  Elbow
  E3.attach(7, 6);      //E3 (W,G)  Wrist
  E4.attach(4, 5);      //E4 (G,W)  Indicator


  delay(10);            //-Allow time for encoders to settle

  //Preset encoders:
  E0.setDegrees(E0_PRESET);     //Frame 0
  E1.setDegrees(E1_PRESET);     //Frame 1
  E2.setDegrees(E2_PRESET);     //Frame 2
  E3.setDegrees(E3_PRESET);     //Frame 3
  E4.setDegrees(E4_PRESET);     //Frame 4

}


// **************** LOOP **************************
void loop()
{
  KinematicChain<10> k; // Declaring a chain with up to 10 links, we'll only be adding 5

  //Read encoders in radians
  double A = E0.getRadians();
  double B = E1.getRadians();
  double C = E2.getRadians();
  double D = E3.getRadians();
  double E = E4.getRadians();

  // Configure the links to give to the kinematic chain (d,Th,r,A)
  RevoluteJoint l1(ARM1, A, 0, M_PI_2);          // F1-0   --- Th0
  RevoluteJoint l2(-18.82, B, ARM2, 0);          // F2-1   --- Th1  //-18.82
  RevoluteJoint l3(0, M_PI_2 + C, 0, M_PI_2);    // F3-2   --- Th2
  RevoluteJoint l4(ARM3, D, 0, M_PI_2);          // F4-3   --- Th3 (Wrist Angle)
  RevoluteJoint l5(-18.82, M_PI_2 + E, ARM4, 0); // F5-4   --- Th4 -18.82

  // Add the Links to the chain and save end point for each link
  k.AddLink(l1);
  double x1 = k.ForwardKinematics().p.X() + X_OFFSET;
  double y1 = k.ForwardKinematics().p.Y() + Y_OFFSET;
  double z1 = k.ForwardKinematics().p.Z() + Z_OFFSET;

  k.AddLink(l2);
  double x2 = k.ForwardKinematics().p.X() + X_OFFSET;
  double y2 = k.ForwardKinematics().p.Y() + Y_OFFSET;
  double z2 = k.ForwardKinematics().p.Z() + Z_OFFSET;

  k.AddLink(l3);
  double x3 = k.ForwardKinematics().p.X() + X_OFFSET;
  double y3 = k.ForwardKinematics().p.Y() + Y_OFFSET;
  double z3 = k.ForwardKinematics().p.Z() + Z_OFFSET;

  k.AddLink(l4);
  double x4 = k.ForwardKinematics().p.X() + X_OFFSET;
  double y4 = k.ForwardKinematics().p.Y() + Y_OFFSET;
  double z4 = k.ForwardKinematics().p.Z() + Z_OFFSET;

  k.AddLink(l5);
  double x = k.ForwardKinematics().p.X() + X_OFFSET;
  double y = k.ForwardKinematics().p.Y() + Y_OFFSET;
  double z = k.ForwardKinematics().p.Z() + Z_OFFSET;


  // Keeps Serial Coms from being overloaded. Sends coordinates after timer runs out
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis > 10)
  {
    // save the last time you blinked the LED
    previousMillis = currentMillis;

    //Send coordinates to Processing

    sendFloat(x, 'x');
    sendFloat(y, 'y');
    sendFloat(z, 'z');
    sendFloat(x1, 'i');
    sendFloat(y1, 'o');
    sendFloat(z1, 'v');
    sendFloat(x2, 'j');
    sendFloat(y2, 'k');
    sendFloat(z2, 'l');
    sendFloat(x3, 'b');
    sendFloat(y3, 'g');
    sendFloat(z3, 'm');
    sendFloat(x4, 'I');
    sendFloat(y4, 'O');
    sendFloat(z4, 'P');

    sendFloat(X_OFFSET, 'E');
    sendFloat(Y_OFFSET, 'R');
    sendFloat(Z_OFFSET, 'T');
  }
/*
       Serial.print ("   *** E0: ");
      Serial.print(E0.getDegrees());
      Serial.print(",  E1: ");
      Serial.print(E1.getDegrees());
      Serial.print(",  E2: ");
      Serial.print(E2.getDegrees());
      Serial.print(",  E3: ");
      Serial.print(E3.getDegrees());
      Serial.print(",  E4: ");
      Serial.println(E4.getDegrees());
      //delay(100);
*/
}
