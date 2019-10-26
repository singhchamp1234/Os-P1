package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
	}
	
	enum Location
	{ 
		Oahu, Molokai;
	} 

    public static void begin( int adults, int children, BoatGrader b )
    {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here 
		static Lock boatLock = new Lock();
		static int numberOfOahuChildren = children;
		static int numberOfOahuAdults = adults;
		static int numberOfMolokaiChildren = 0;
		static int numberOfMolokaiAdults = 0;
		static Location boatLocation = Oahu;
		static Location personLocation = Oahu;
		static int numberOfPassengers = 0;
		static Condition waitOnMolokai = new Condition(boatLock);
		static Condition waitOnOahu = new Condition(boatLock);
		static Condition waitFullBoat = new Condition(boatLock);
		static Communicator communicator = new Communicator();

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		Runnable r = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};

		for (int i = 0; i < children; i++) {
			KThread person = new KThread(r_child);
			person.fork();
		}
		for (int i = 0; i < adults; i++) {
			KThread person = new KThread(r_adult);
			person.fork();
		}
    }

    static void AdultItinerary()
    {
		boatLock.acquire();
		if (adult is on Oahu) {
			if (numberOfPassengers = 2) {
				waitFullBoat.sleep();
			} else if (boatLocation != Oahu || numberOfOahuChildren > 1) {
				waitOnOahu.sleep();
			} else {
				numberOfPassengers = 2;
				bg.AdultRowToMolokai();
				waitOnMolokai.wakeAll() // Wake up a child sleeping to ride back
				waitOnMolokai.sleep();
			}
		} else (adult is on Molokai) {
			waitOnMolokai.sleep();
		}
		boatLock.release();
    }

    static void ChildItinerary()
    {
		boatLock.acquire();
		if (child is on Oahu) {
			if (numberOfPassengers = 2) {
				waitFullBoat.sleep();
			} else if (numberOfPassengers = 1 && boatLocation = Oahu) {
				numberOfPassengers = 2;
				bg.ChildRideToMolokai();
				waitOnMolokai.sleep();
			} else if (boatLocation != Oahu || numberOfOahuChildren = 1) {
				waitOnOahu.wakeAll(); // Wake up all the adults sleeping
				waitOnOahu.sleep();
			} else {
				numberOfPassengers = 1; // This lets the child you wake up to ride
				Record numberOfOahuChildren and numberOfOahuAdults
				waitOnOahu.wakeAll(); // wakes up a child to ride
				bg.ChildRowToMolokai()
				if (numberOfOahuChildren > 0 || numberOfOahuAdults > 0) {
					Wake up a child sleeping with waitOnMolokai
					waitOnMolokai.sleep();
				} else {
					Notify simulation is over with Communicator
				}
			}
		} else (child is on Molokai) {
			if (numberOfPassengers == 0 && boatLocation == Oahu) {
				numberOfPassengers = 1;
				bg.ChildRowtoOahu();
				waitFullBoat.wakeAll();
				waitOnOahu.wakeAll();
				waitOnOahu.sleep();
			} else {
				waitOnMolokai.sleep();
			}
		}
		boatLock.release();	
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
