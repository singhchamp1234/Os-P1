package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.*;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);

		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);
		/*
		System.out.println("\n ***Testing Boats with 2 children, 5 adults***");
		begin(5, 2, b);

		System.out.println("\n ***Testing Boats with 9 children, 6 adults***");
		begin(6, 9, b);

		System.out.println("\n ***Testing Boats with 2 children, 10 adults***");
		begin(10, 2, b);

		System.out.println("\n ***Testing Boats with 21 children, 11 adults***");
		begin(11, 21, b);

		System.out.println("\n ***Testing Boats with 100 children, 100 adults***");
		begin(100, 100, b);

		System.out.println("\n ***Testing Boats with 998 children, 1000 adults***");
		begin(1000, 998, b);

		System.out.println("\n ***Testing Boats with 37 children, 37 adults***");
		begin(37, 37, b);

		System.out.println("\n ***Testing Boats with 37 children, 50 adults***");
		begin(50, 37, b);

		System.out.println("\n ***Testing Boats with 50 children, 37 adults***");
		begin(37, 50, b);
		*/
	}
	
	enum Location
	{ 
		Oahu, Molokai;
	} 
	static Lock boatLock;
	static int numberOfOahuChildren;
	static int numberOfOahuAdults;
	static int numberOfMolokaiChildren;
	static int numberOfMolokaiAdults;
	static Location boatLocation;
	static int numberOfPassengers;
	static Condition2 waitOnMolokai;
	static Condition2 waitOnOahu;
	static Communicator communicator;

    public static void begin( int adults, int children, BoatGrader b )
    {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here 
		boatLock = new Lock();
		numberOfOahuChildren = children;
		numberOfOahuAdults = adults;
		numberOfMolokaiChildren = 0;
		numberOfMolokaiAdults = 0;
		boatLocation = Location.Oahu;
		numberOfPassengers = 0;
		waitOnMolokai = new Condition2(boatLock);
		waitOnOahu = new Condition2(boatLock);
		communicator = new Communicator();

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		Runnable runChild = new Runnable() {
			public void run() {
				Location adultLocation = Location.Oahu;
				ChildItinerary(adultLocation);
			}
		};

		Runnable runAdult = new Runnable() {
			public void run() {
				Location childLocation = Location.Oahu;
				AdultItinerary(childLocation);
			}
		};

		for (int i = 0; i < children; i++) {
			KThread person = new KThread(runChild);
			person.setName("Child " + (i + 1));
			person.fork();
		}
		for (int i = 0; i < adults; i++) {
			KThread person = new KThread(runAdult);
			person.setName("Adult " + (i + 1));
			person.fork();
		}

		int numberOfMolokaiPeople = communicator.listen();
		//System.out.println("All " + numberOfMolokaiPeople + " of the children and adults are all now on Molokai.");
    }

    static void AdultItinerary(Location adultLocation)
    {
		while (true) {
			boatLock.acquire();
			// Boat location to determine which island the adult is on as he/she will only be woken up by someone who just arrived to corresponding island or while boat is still on island
			if (adultLocation == Location.Oahu && boatLocation == Location.Oahu) {
				// Only row to Molokai if 1 child is left on Oahu
				if (numberOfOahuChildren == 1) {

					// Row to Molokai
					numberOfOahuAdults--;
					bg.AdultRowToMolokai();
					numberOfMolokaiAdults++;
					boatLocation = Location.Molokai;
					adultLocation = Location.Molokai;

					// Wake up a child to row back to Oahu (to pair up with the 1 child on Oahu; if adult is woken up he/she will wake up a child)
					waitOnMolokai.wake();

					waitOnMolokai.sleep();
				} else if (numberOfOahuChildren > 1 || numberOfPassengers == 1) {
					// Wake up a child if there is a pair of children on Oahu and/or a child is supposed to ride to Molokai
					waitOnOahu.wake();

					waitOnOahu.sleep();
				}
			} else if (adultLocation == Location.Molokai && boatLocation == Location.Molokai) {
				// If an adult is woken up he/she will wake up a child to row back to Oahu
				waitOnMolokai.wake();

				waitOnMolokai.sleep();
			} else { // adult and boat location are not same
				if (adultLocation == Location.Oahu) {
					waitOnOahu.sleep();
				} else if (adultLocation == Location.Molokai) {
					waitOnMolokai.sleep();
				}
			}
			boatLock.release();
		}
    }

    static void ChildItinerary(Location childLocation)
    {
		while (true) {
			boatLock.acquire();
			// Boat location to determine which island the child is on as he/she will only be woken up by someone who just arrived to corresponding island or while boat is still on island
			if (childLocation == Location.Oahu && boatLocation == Location.Oahu) {
				if (numberOfOahuChildren >= 1 && numberOfPassengers == 0) {	// Rower
					/// Only pairs of children go from Oahu to Molokai, due to solution of problem
					// This lets the child you wake up to ride
					numberOfPassengers = 1;

					// Wake up a child to ride to Molokai
					waitOnOahu.wake();

					// Row to Molokai
					numberOfOahuChildren--;
					bg.ChildRowToMolokai();
					numberOfMolokaiChildren++;
					childLocation = Location.Molokai;
					waitOnMolokai.sleep();
				} else if (numberOfPassengers == 1) { // Rider
					// Ride to Molokai
					numberOfOahuChildren--;
					bg.ChildRideToMolokai();
					numberOfMolokaiChildren++;
					boatLocation = Location.Molokai; // Since RideToMolokai is called after RowtoMolokai, the passenger/rider will update boatLocation for Rower.
					childLocation = Location.Molokai;
					numberOfPassengers = 0;

					// Child just rode from Oahu so it should know/remember number of children and adults
					if (numberOfOahuChildren > 0 || numberOfOahuAdults > 0) {
						// Wake up a child to ride back to Oahu
						waitOnMolokai.wake();

						waitOnMolokai.sleep();
					} else {
						// Report back the total number of adults and children on Molokai and end of simulation
						communicator.speak(numberOfMolokaiAdults + numberOfMolokaiChildren);

						waitOnOahu.sleep();
					}
				} else if (numberOfOahuChildren == 1) {
					// Wake up an adult sleeping on Oahu to row to Molokai
					waitOnOahu.wake();

					waitOnOahu.sleep();
				}
			} else if (childLocation == Location.Molokai && boatLocation == Location.Molokai) {
				// If a child is woken up, it means he/she is tasked to ride back alone to Oahu
				// Row to Oahu
				numberOfMolokaiChildren--;
				bg.ChildRowToOahu();
				numberOfOahuChildren++;
				boatLocation = Location.Oahu;
				childLocation = Location.Oahu;

				// Wake up a person sleeping on Oahu
				waitOnOahu.wake();
				
				waitOnOahu.sleep();
			} else { // child and boat location are not same
				if (childLocation == Location.Oahu) {
					waitOnOahu.sleep();
				} else if (childLocation == Location.Molokai) {
					waitOnMolokai.sleep();
				}
			}
			boatLock.release();
		}
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
