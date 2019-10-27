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

		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);

		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);

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
	static Condition waitOnMolokai;
	static Condition waitOnOahu;
	static Condition waitFullBoat;
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
		boatLocation = Oahu;
		numberOfPassengers = 0;
		waitOnMolokai = new Condition(boatLock);
		waitOnOahu = new Condition(boatLock);
		communicator = new Communicator();

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		Runnable runChild = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};

		Runnable runAdult = new Runnable() {
			public void run() {
				AdultItinerary();
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
		System.out.println("All " + numberOfMolokaiPeople + " of the children and adults are all now on Molokai.");
    }

    static void AdultItinerary()
    {
		boatLock.acquire();
		// Boat location to determine which island the adult is on as he/she will only be woken up by someone who just arrived to corresponding island
		if (boatLocation == Oahu) {
			// Only ride to Molokai if 1 child is left on Oahu
			if (numberOfOahuChildren == 1) {
				numberOfOahuAdults--;
				bg.AdultRowToMolokai();
				numberOfMolokaiAdults++;
				boatLocation = Molokai;
				waitOnMolokai.wake(); // Wake up a child to row back to Oahu (to pair up with the 1 child on Oahu; if adult is woken up he/she will wake up a child)
				waitOnMolokai.sleep();
			} else if (numberOfOahuChildren > 1) {
				waitOnOahu.sleep();
			}
		} else if (boatLocation == Molokai) {
			// if an adult is woken up he/she will wake up a child to row back to Oahu
			waitOnMolokai.wake();
			waitOnMolokai.sleep();
		}
		boatLock.release();
    }

    static void ChildItinerary()
    {
		boatLock.acquire();
		// Boat location to determine which island the child is on as he/she will only be woken up by someone who just arrived to corresponding island
		if (boatLocation == Oahu) {
			if (numberOfOahuChildren >= 1 && numberOfPassengers == 0) {
				// Only pairs of children go from Oahu to Molokai, due to solution of problem
				numberOfPassengers = 1;
				waitOnOahu.wakeAll(); // Wake up a child to ride to Molokai
				bg.ChildRowToMolokai();
				numberOfMolokaiChildren++;
				waitOnMolokai.sleep();
			} else if (numberOfOahuChildren == 1) {
				waitOnOahu.wakeAll(); // Wake up all the adults sleeping on Oahu
				waitOnOahu.sleep();
			} else if (numberOfPassengers == 1) {
				// since RideToMolokai is called after RowtoMolokai, the passenger will handle the numberOfOahuChilren count and updating of boatLocation for Rower.
				numberOfOahuChildren -= 2;
				bg.ChildRideToMolokai();
				numberOfMolokaiChildren++;
				boatLocation = Molokai;
				numberOfPassengers = 0;
				// Child just rode from Oahu so it should know/remember number of children and adults
				if (numberOfOahuChildren > 0 || numberOfOahuAdults > 0) {
					waitOnMolokai.wakeAll(); // Wake up a child to ride back to Oahu
					waitOnMolokai.sleep();
				} else {
					// Report back the total number of adults and children on Molokai and end of simulation
					communicator.speak(numberOfMolokaiAdults + numberOfMolokaiChildren);
				}
			}
		} else if (boatLocation == Molokai) {
			// if a child is woken up, it means he/she is tasked to ride back alone to Oahu
			numberOfMolokaiChildren--;
			bg.ChildRowtoOahu();
			numberOfOahuChildren++;
			boatLocation = Oahu;
			waitOnOahu.wakeAll();
			waitOnOahu.sleep();
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
