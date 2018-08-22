 /*********************************************
 *  Agent.java 
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411/9414/9814 Artificial Intelligence
 *  UNSW Session 1, 2018
 *  Jeremy Chen (z5016815), Jeremy Yao (z3463470)
 *  How the Program Works:
 *  
 *  Firstly, we created a World Map of the 
 *  current Location. This World Map records 
 *  every location that we have explored. 
 *  After creating a world map, we use a 
 *  variation of the Flood-Fill Algorithm to 
 *  Explore the surrounding Areas. This 
 *  Algorithm visits every square on the Map 
 *  possible, allowing us to explore every 
 *  corner of the map as well as pick up all 
 *  tools in our area. Upon fully searching 
 *  the map available to us, we then proceed 
 *  to use A* to path to any obstructions such 
 *  as a door or tree (if we have a key/axe), 
 *  where we can use our tools. If it can’t 
 *  find a Tree or a Door, then it will 
 *  attempt to move across water if the agent 
 *  possesses either a Stone or a Raft, 
 *  prioritising the usage of Stone. 

	Upon using the tool to move through any 
	particular obstruction, the Agent would 
	then proceed to move forward and explore 
	the new area. After exploring the new 
	area it would then repeat the cycle. It 
	would try to open doors or cut down 
	trees. If it couldn’t cut down any trees 
	or doors, but if it had additional 
	stones, it would try to place them in 
	strategic locations. We used Greedy 
	Search to see if placing the stones could 
	achieve any particular objective, this 
	search is faster than A* Search as we 
	don’t need an optimal solution. If it no 
	longer had any more stones at its 
	disposal, then it would attempt to use the 
	raft if it had one. It would check using 
	Greedy Search to see if placing the Raft 
	at that specific location would net any 
	objective. If it can see an objective, it 
	will use A* to path to that route 
	(prioritising some objects such as Tree 
	First). Upon finding a treasure, it will 
	then attempt to path back to the starting 
	point using A*. 

 */

 
import java.util.*;
import java.io.*;
import java.net.*;

public class Agent {

    //debugging variables
    public static int debugSpeed = 5; //lower is faster
    public static boolean isDebugging = false;
    public static long timeStart = System.currentTimeMillis();

    //General Variables
    public static int startFlag = 0; 										//Checks if first iteration 
    public static char lastdirection = 's';									//Finds the last direction command
    public static int direction = 2;										//Finds current direction
    public final static int mapsize = 160;
    public static char map[][] = new char[mapsize][mapsize]; 				//Map of Layout
    public static int stateX = (mapsize - 4) / 2; 							//Current X Location on Map
    public static int stateY = (mapsize - 4) / 2; 							//Current Y Location on Map
    public static int initialsX = mapsize / 2;								//Start Location X
    public static int initialsY = mapsize / 2;								//Start Location Y

    //Exploration Variables
    public static int movement = 0;
    public static char visitedmap[][] = new char[mapsize][mapsize];			//Records All Visited Locations
    public static Stack < Integer > XStack = new Stack < Integer > ();		//Stack of Visited Locations (X-Coordinate)
    public static Stack < Integer > YStack = new Stack < Integer > ();		
    public static int backtrackX = 0;										//Backtrack Location 
    public static int backtrackY = 0;

    //Tool Using Variables
    public static boolean doneExploring = false;							//Checks if Done Exploring
    public static int key = 0;												//Number of Keys				
    public static int axe = 0;												//If we have an Axe
    public static int treasure = 0;											//If we have the Treasure	
    public static boolean raft = false;										//If we have a Raft	
    public static int stone = 0;											//How many Stones we have

    //Stone Placement Variables
    public static int candidate[] = {-1,-1};								//Candidate For Stone Placement
    public static boolean prereqs = false;									//If we have completed all Non-Water Tasks	
    public static char visitedwater[][] = new char[mapsize][mapsize];		//All Visited Locations on Water
    public static char originals[][] = new char[mapsize][mapsize];			//Original Locations before Visited
    public static int priority[][] = new int[mapsize][mapsize];				//Sorts the Priority of All Stone Locations, to see which is Optimal
    public static int CantorX[] = new int[12960];							//Cantor Pairing Function when Stones > 1
    public static int CantorY[] = new int[12960];
    public static int placeholder = 0;
    public static int UseX = 0;												//Placeholder Values			
    public static int UseY = 0;

    //Variables for Finding Raft Position for Embarking
    public static boolean rafting = false;									//If on a Raft
    public static boolean raftsearch = false;								//Checks if Finished Water Exploration
    public static char visitedraft[][] = new char[mapsize][mapsize];		//All Visited Locations via Raft
    public static int raftcandidate[] = {-1,-1};							//Candidate Locations for initiating a Raft Placement
    public static int raftpriority[][] = new int[mapsize][mapsize];			//Prioritising Locations to use a Raft
	public static boolean Embark = false;

    //Variables for Finding Raft Position for Disembarking
    public static char visitedbeyond[][] = new char[mapsize][mapsize];		//Visited Locations via Water Beyond current Locations
    public static int beyondpriority[][] = new int[mapsize][mapsize];		//Prioritises Optimal Disembarking Locations
    public static int beyondcandidate[] = {-1,-1};							//Optimal Candidate Location for Disembarking
    public static boolean ready = false;									//Disembark Location Found
    public static int AlreadyBeen = 0;										//Locations Already Been To

    //variables for A*
    public static final int ASTAR = 0;
    public static final int GREEDY = 1;
    
    //variables for go_to_object and can_go_to_object
    public static final int BEYOND = 0;
    public static final int RAFT   = 1;
    public static final int STONE  = 2;
    public static final int LAND   = 3;

    /**
     * STRATEGY
     * 
     * STEP 1 - EXPLORE ENTIRE AREA AND COLLECT TOOLS ALONG THE WAY
     * 
     * STEP 2 - USE TOOLS (HOLD THE STONE UNTIL ALL OTHER OPTIONS EXHAUSTED) THEN EXPLORE THE NEW AREA
     * 
     * STEP 3 - PLACE STONES
	 *
	 * STEP 4 - FIND A SUITABLE AREA TO USE THE RAFT
     * 
	 * STEP 5 - USE THE RAFT AND NAVIGATE TO A NEW AREA
	 * 
     * @param view
     * @return
     */

    public char get_action(char view[][]) {

        try {															//Adding delay to see changes
            Thread.sleep(debugSpeed);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        
        int i, j;
        int[] statePos = {stateY,stateX};
        if (ready == true) {											//After it has found Destination to go to via Raft, it will be ready to Go
            return (char) go_to_object(visitedbeyond, 'B', BEYOND);
        }
        if (raftsearch == true) {										//While on a Raft and after we have finished Exploring, Finds where we need to Go Next, sorted by Priority
            return raftDisembarking();
        }
		if (Embark == true){											//Found a Location to Start using the Raft
			return (char) go_to_object(map, 'G', LAND);
		}
		/** FINDING A LOCATION TO PLACE THE RAFT 
		
		    - First we create a Map called VisitedRaft which is a copy of Map (we will need to modify that for this Task)
			- We set all the Unexplored Areas as 'N', which is Unpathable by A*
		 	- Every Previously Visited Location on VisitedRaft is Denoted by X.
		
		**/

        if (prereqs && doneExploring == true && stone == 0 && rafting == true) {  
            //checks if we all stones have been placed, we have no more stones in inventory, we have finished exploration and if we can raft
			for (i = 0; i < raftpriority.length; i++) {
                for (j = 0; j < raftpriority.length; j++) {
                    raftpriority[i][j] = 0;
                }
            }
			//instatiate the Map 
			for (i = 0; i < visitedraft.length; i++) {
                for (j = 0; j < visitedraft.length; j++) {
					if (map[i][j] == '~'){
						visitedraft[i][j] = '~';
					}
				}
			}
			//Find a List of Candidate Positions to begin searching for a Raft Position. Water Locations that are next to Land
            for (i = 0; i < visitedraft.length; i++) {
                for (j = 0; j < visitedraft.length; j++) {
                    if (visitedraft[i][j] == '~') {
                        if (visitedraft[i + 1][j] == 'X' || visitedraft[i - 1][j] == 'X' || visitedraft[i][j + 1] == 'X' || visitedraft[i][j - 1] == 'X'
						|| map[i + 1][j] == ' ' || map[i - 1][j] == ' ' || map[i][j + 1] == ' ' || map[i][j - 1] == ' '
						) {
							visitedraft[i][j] = 'E';
						}
					}
				}
			}
			//We Now have a list of Possible Candidate Locations denoted by E on the Map
			int[] Empty = {-1,-1};
			//Now for these Candidate Locations, we check if we can reach them currently. 
			while (!scan_pos(visitedraft, 'E', 1).equals(Empty)){
				int JPos[] = scan_pos(visitedraft, 'E', 1);
				if (JPos[0] == -1 || JPos[1] == -1){
					break;
				}
                int First = JPos[0];
                int Second = JPos[1];
				visitedraft[First][Second] = 'S'; 
                if (can_go_to_object(visitedraft, statePos, 'S', RAFT) == true) {
					visitedraft[First][Second] = 'J';
				}
				else {
					visitedraft[First][Second] = '~';
				}
			}	
			//Once we have a list of viable Candidates, we have to invert the Map, so that the Land is unpathable and the Water is Pathable
			for (i = 0; i < visitedraft.length; i++) {
                for (j = 0; j < visitedraft.length; j++) {
                    if (visitedraft[i][j] == 'X') {
                        visitedraft[i][j] = 'H';
                    }
                }
            }
			
			for (i = 0; i < visitedraft.length; i++) {
                for (j = 0; j < visitedraft.length; j++) {
                    if (visitedraft[i][j] == '~') {
                        visitedraft[i][j] = '&';
                    }
                }
            }
			//Edge case scenario, if there is nothing visible we can reach, but there is a water area near an Unexplored Area, we go there
			for (i = 0; i < visitedraft.length; i++) {
				for (j = 0; j < visitedraft.length; j++) {
					if (visitedraft[i][j] == '&') {
						if (visitedraft[i + 1][j] == 'N' || visitedraft[i - 1][j] == 'N' || visitedraft[i][j + 1] == 'N' || visitedraft[i][j - 1] == 'N') {
							visitedraft[i][j] = 'R';
						}
					}
				}
			}
			//Now for each viable candidate position we must see if it reaches the a certain Objective 
            while (!scan_pos(visitedraft, 'J', 1).equals(Empty)){ 
				int JPos[] = scan_pos(visitedraft, 'J', 1);
				if (JPos[0] == -1 || JPos[1] == -1){
					break;
				}
                int First = JPos[0];
                int Second = JPos[1];
				int newPos[] = {First,Second};
				//Modification so that we can't Use Raft Twice on the Same Path
				//Basically, we only path to water that has been connected to J.
				//We set all connected water to S, then invert the unconnected water to E (Unpathable Terrain)
				if (visitedraft[First+1][Second] == '&'){
					visitedraft[First+1][Second] = 'S';	
				}
				if (visitedraft[First-1][Second] == '&'){
					visitedraft[First-1][Second] = 'S';
				}
				if (visitedraft[First][Second+1] == '&'){
					visitedraft[First][Second+1] = 'S';
				}
				if (visitedraft[First][Second-1] == '&'){
					visitedraft[First][Second-1] = 'S';
				}
				while (Connected() == false){}
				for (i = 0; i < visitedraft.length; i++) {
					for (j = 0; j < visitedraft.length; j++) {
						if (visitedraft[i][j] == '&') {
							visitedraft[i][j] = 'E';
						}
					}
				}
                visitedraft[First][Second] = 'S'; 
				// Now we check if we can path to any particular objectives, these objectives are ranked
				// While Rafting, the tree is our primary objective as it provides us with a Return route
				// This is followed by some other objectives such as Tools or Doors.
                if (can_go_to_object(visitedraft, newPos, 'T', RAFT) == true) {
                    raftpriority[First][Second] = 4; 
                } else if (can_go_to_object(visitedraft, newPos, 'o', RAFT) == true) {
                    raftpriority[First][Second] = 4;
                } else if (can_go_to_object(visitedraft, newPos, 'k', RAFT) == true) {
                    raftpriority[First][Second] = 3;
				} else if (can_go_to_object(visitedraft, newPos, '-', RAFT) == true && key > 0) {
                    raftpriority[First][Second] = 3;
                } else if (can_go_to_object(visitedraft, newPos, 'M', RAFT) == true && treasure > 0) {
                    raftpriority[First][Second] = 2;
                } else if (can_go_to_object(visitedraft, newPos, ' ', RAFT) == true) {
                    raftpriority[First][Second] = 1;
                } else if (can_go_to_object(visitedraft, newPos, 'R', RAFT) == true) {
                    raftpriority[First][Second] = 1;
				}
                visitedraft[First][Second] = '*'; 
				//We need to reverse our changes for the next loop iteration
				for (i = 0; i < visitedraft.length; i++) {
					for (j = 0; j < visitedraft.length; j++) {
						if (visitedraft[i][j] == 'E' || visitedraft[i][j] == 'S') {
							visitedraft[i][j] = '&';
						}
					}
				}	
            }
			//Now we check if there is an Objective with a Priority of 4 and Return if there is, we then go there
			if (RaftPrioritising(4) == true){
				Embark = true;
				return 0;
			}
			//Now we check if there is an Objective with a Priority of 3 and Return if there is, we then go there
			if (RaftPrioritising(3) == true){
				Embark = true;
				return 0;
			}
			//Now we check if there is an Objective with a Priority of 2 and Return if there is, we then go there
            if (RaftPrioritising(2) == true){
				Embark = true;
				return 0;
			}
			//Now we check if there is an Objective with a Priority of 1 and Return if there is, we then go there
			if (RaftPrioritising(1) == true){
				Embark = true;
				return 0;
			}
        }
        //prereqs if all non-water tasks are completed
		
		/**
		Finding A Location to Place the Stone
		
		**/
		
		//Resetting all the current priorities
        if (prereqs && doneExploring == true && stone > 0) {
            for (i = 0; i < priority.length; i++) {
                for (j = 0; j < priority.length; j++) {
                    priority[i][j] = 0;
                }
            }
            for (i = 0; i < visitedwater.length; i++) {
                for (j = 0; j < visitedwater.length; j++) {
                    if (originals[i][j] == '~') {
                        visitedwater[i][j] = '~';
                    }
                }
            }
            boolean found = false;
            boolean shortcut = false;
			//Finds all possible Stone Locations That can be placed
            for (i = 0; i < visitedwater.length; i++) {
                for (j = 0; j < visitedwater.length; j++) {
                    if (visitedwater[i][j] == '~') {
                        if (visitedwater[i + 1][j] == 'X' || visitedwater[i - 1][j] == 'X' || visitedwater[i][j + 1] == 'X' || visitedwater[i][j - 1] == 'X') {

                            if (visitedwater[i + 1][j] != 'X' && visitedwater[i + 1][j] != '*') {
                                visitedwater[i][j] = 'E';
                            }
                            if (visitedwater[i - 1][j] != 'X' && visitedwater[i - 1][j] != '*') {
                                visitedwater[i][j] = 'E';
                            }
                            if (visitedwater[i][j + 1] != 'X' && visitedwater[i][j + 1] != '*') {
                                visitedwater[i][j] = 'E';
                            }
                            if (visitedwater[i][j - 1] != 'X' && visitedwater[i][j - 1] != '*') {
                                visitedwater[i][j] = 'E';
                            }
                        }
                    }
                }
            }
			//Checks the viability of those stone locations and whether they lead to an actual goal
            while (can_go_to_object(visitedwater, statePos, 'E', STONE) == true) {
                int First = candidate[0];
                int Second = candidate[1];
                visitedwater[candidate[0]][candidate[1]] = 'P';
                priority[First][Second] = 1;
                if (can_go_to_object(visitedwater, statePos, 'o', STONE) == true) {
                    foundStone(First, Second);
                    found = true;
                    break;
                } else if (can_go_to_object(visitedwater, statePos, '-', STONE) == true && key > 0) {
                    foundStone(First, Second);
                    found = true;
                    break;
                } else if (can_go_to_object(visitedwater, statePos, 'T', STONE) == true && axe > 0) {
                    foundStone(First, Second);
                    found = true;
                    break;
                } else if (can_go_to_object(visitedwater, statePos, 'k', STONE) == true) {
                    foundStone(First, Second);
                    found = true;
                    break;
                } else if (can_go_to_object(visitedwater, statePos, 'a', STONE) == true) {
                    foundStone(First, Second);
                    found = true;
                    break;
                } else if (can_go_to_object(visitedwater, statePos, '$', STONE) == true) {
                    found = true;
                    priority[First][Second] = 4;
                } else if (can_go_to_object(visitedwater, statePos, ' ', STONE) == true) {
                    found = true; 
                    priority[First][Second] = 3;
                }
                visitedwater[First][Second] = '#';
            }
			//For an Edge case where we are moving back over a stone
            int path = stone;
            int fixedpath = path;
            boolean singlestone = true;
            for (i = 0; i < visitedwater.length; i++) {
                for (j = 0; j < visitedwater.length; j++) {
                    if (visitedwater[i][j] == '#') {
                        if (visitedwater[i + 1][j] == '~') {
                            singlestone = false;
                        } else if (visitedwater[i - 1][j] == '~') {
                            singlestone = false;
                        } else if (visitedwater[i][j + 1] == '~') {
                            singlestone = false;
                        } else if (visitedwater[i][j - 1] == '~') {
                            singlestone = false;
                        }
                    }
                }
            }
            while (path > 1 && found == false && singlestone == false) {
                int pathcount = fixedpath + 1;
                for (i = 0; i < visitedwater.length; i++) {
                    for (j = 0; j < visitedwater.length; j++) {
                        if (visitedwater[i][j] == '#') {
                            visitedwater[i][j] = '=';
                        }
                    }
                }
				//Uses Cantor Pairing Function to Link Multiple stones together from a similar (X,Y) co-ordinate
				//This Section of the code is for dealing with multiple stones
                if (can_go_to_object(visitedwater, statePos, '~', STONE) == true) {
                    int OriginalFirst = candidate[0];
                    int OriginalSecond = candidate[1];
                    int First = candidate[0];
                    int Second = candidate[1];
                    visitedwater[candidate[0]][candidate[1]] = 'P';
                    int value = (((First + Second) * (First + Second + 1)) / 2) + Second;

                    if (visitedwater[First + 1][Second] == '=') {
                        CantorX[value] = First + 1;
                        CantorY[value] = Second;
                    } else if (visitedwater[First - 1][Second] == '=') {
                        CantorX[value] = First - 1;
                        CantorY[value] = Second;
                    } else if (visitedwater[First][Second + 1] == '=') {
                        CantorX[value] = First;
                        CantorY[value] = Second + 1;
                    } else if (visitedwater[First][Second - 1] == '=') {
                        CantorX[value] = First;
                        CantorY[value] = Second - 1;
                    }
                    int currVal = value;
					//Searching Back through the Cantor Array to find the origin point of the stone
                    if (can_go_to_object(visitedwater, statePos, 'o', STONE) == true) {
                        Cantor(5, currVal);
                        path--;
                    } else if (can_go_to_object(visitedwater, statePos, '-', STONE) == true && key > 0) {
                        Cantor(5, currVal);
                        path--;
                    } else if (can_go_to_object(visitedwater, statePos, 'T', STONE) == true && axe > 0) {
                        Cantor(5, currVal);
                        path--;
                    } else if (can_go_to_object(visitedwater, statePos, 'k', STONE) == true) {
                        Cantor(5, currVal);
                        path--;
                    } else if (can_go_to_object(visitedwater, statePos, 'a', STONE) == true) {
                        Cantor(5, currVal);
                        path--;
                    } else if (can_go_to_object(visitedwater, statePos, '$', STONE) == true) {
                        Cantor(4, currVal);
                        path--;
                    } else if (can_go_to_object(visitedwater, statePos, ' ', STONE) == true) {
                        Cantor(3, currVal);
                        path--;
                    }
                    visitedwater[OriginalFirst][OriginalSecond] = '#';
                }
            }
			// Finding objectives with a priority level of 5. If it can't find these, then it begins to seek out objectives of a lower priority level
            if (Prioritising(5) == true) {
                return 0;
            }
            if (Prioritising(4) == true) {
                return 0;
            }
            if (Prioritising(3) == true) {
                return 0;
            }
            if (Prioritising(1) == true) {
                return 0;
            }
        }

        if (doneExploring) {
            return landTravel();
        }

        //SCANNING ENTIRE INITIAL AREA (STEP 1)

        return explore(view);
    }

    /**
     * Explores the unexplored area
     * 
     * @param view
     * @return
     */
    char explore(char[][] view) {
        prereqs = false;

        char front = view[1][2];
        char right = view[2][3];
        char back = view[3][2];
        char left = view[2][1];

        char mapfront = visitedmap[stateY - 1][stateX];
        char mapright = visitedmap[stateY][stateX + 1];
        char mapback = visitedmap[stateY + 1][stateX];
        char mapleft = visitedmap[stateY][stateX - 1];
        //re-adjusting for direction
        if (direction == 0) {
            mapfront = visitedmap[stateY - 1][stateX];
            mapright = visitedmap[stateY][stateX + 1];
            mapback = visitedmap[stateY + 1][stateX];
            mapleft = visitedmap[stateY][stateX - 1];
        } else if (direction == 1) {
            mapfront = visitedmap[stateY][stateX + 1];
            mapright = visitedmap[stateY + 1][stateX];
            mapback = visitedmap[stateY][stateX - 1];
            mapleft = visitedmap[stateY - 1][stateX];
        } else if (direction == 2) {
            mapfront = visitedmap[stateY + 1][stateX];
            mapright = visitedmap[stateY][stateX - 1];
            mapback = visitedmap[stateY - 1][stateX];
            mapleft = visitedmap[stateY][stateX + 1];
        } else if (direction == 3) {
            mapfront = visitedmap[stateY][stateX - 1];
            mapright = visitedmap[stateY - 1][stateX];
            mapback = visitedmap[stateY][stateX + 1];
            mapleft = visitedmap[stateY + 1][stateX];
        }
        //used for backtracking back when no other options for movement is available
        if (movement == 4) {
            movement--;
            return 'r';
        }
        if (movement == 3) {
            movement = 0;
            return 'f';
        }
        //used for turning 180 degrees
        if (movement == 2) {
            movement--;
            return 'r';
        }
        //used for turning 90 degrees
        if (movement == 1) {
            movement = 0;
            return 'f';
        }
        //checks if can move forward, then moves forward and pushes current location to stack
        if (front == '~' && mapfront != 'X' && rafting == true) {
            XStack.push(stateX);
            YStack.push(stateY);
            return 'f';
        }
        //checks if can move forward, then moves forward and pushes current location to stack
        if (front != '-' && front != '*' && front != 'T' && front != '.' && front != '~' && mapfront != 'X' && rafting == false) {
            VariableUpdate(front);
            return 'f';
        }
        //checks if can move right, then turns right and sets movement to 1, so that the next movement is 'f'
        if (right == '~' && mapright != 'X' && rafting == true) {
            XStack.push(stateX);
            YStack.push(stateY);
            movement = 1;
            return 'r';
        }
        //checks if can move right, then turns right and sets movement to 1, so that the next movement is 'f'
        if (right != '-' && right != '*' && right != 'T' && right != '.' && right != '~' && mapright != 'X' && rafting == false) {
            VariableUpdate(right);
            XStack.push(stateX);
            YStack.push(stateY);
            movement = 1;
            return 'r';
        }
        //checks if can move back, then turns right and sets movement to 2, so that the next movement is 'r' followed by 'f'
        if (back == '~' && mapback != 'X' && rafting == true) {
            XStack.push(stateX);
            YStack.push(stateY);
            movement = 2;
            return 'r';
        }
        //checks if can move back, then turns right and sets movement to 2, so that the next movement is 'r' followed by 'f'
        if (back != '-' && back != '*' && back != 'T' && back != '.' && back != '~' && mapback != 'X' && rafting == false) {
            VariableUpdate(back);
            XStack.push(stateX);
            YStack.push(stateY);
            movement = 2;
            return 'r';
        }
        //checks if can move left, then turns left and sets movement to 1, so that the next movement is 'f'
        if (left == '~' && mapleft != 'X' && rafting == true) {
            XStack.push(stateX);
            YStack.push(stateY);
            movement = 1;
            return 'l';
        }
        //checks if can move left, then turns left and sets movement to 1, so that the next movement is 'f'
        if (left != '-' && left != '*' && left != 'T' && left != '.' && left != '~' && mapleft != 'X' && rafting == false) {
            VariableUpdate(left);
            XStack.push(stateX);
            YStack.push(stateY);
            movement = 1;
            return 'l';
        } else {
            //stops moving once the stack is empty and has finished exploring
            if (XStack.isEmpty()) {
                if (rafting == true) {
                    raftsearch = true;
                    AlreadyBeen++;
                }
                doneExploring = true;
                return 0;
            }
            //no possible moves available, pops off stack and backtracks to previous location
            backtrackX = (int) XStack.pop();
            backtrackY = (int) YStack.pop();
            //find which location to move back to, using same methodology as above (with adjustments for direction)
            if (backtrackX == stateX - 1) {
                if (direction == 0) {
                    movement = 1;
                    return 'l';
                } else if (direction == 1) {
                    movement = 4;
                    return 'r';
                } else if (direction == 2) {
                    movement = 1;
                    return 'r';
                } else if (direction == 3) {
                    return 'f';
                }
            }
            if (backtrackX == stateX + 1) {
                if (direction == 0) {
                    movement = 1;
                    return 'r';
                } else if (direction == 1) {
                    return 'f';
                } else if (direction == 2) {
                    movement = 1;
                    return 'l';
                } else if (direction == 3) {
                    movement = 4;
                    return 'r';
                }
            }
            if (backtrackY == stateY - 1) {
                if (direction == 0) {
                    return 'f';
                } else if (direction == 1) {
                    movement = 1;
                    return 'l';
                } else if (direction == 2) {
                    movement = 4;
                    return 'r';
                } else if (direction == 3) {
                    movement = 1;
                    return 'r';
                }
            }
            if (backtrackY == stateY + 1) {
                if (direction == 0) {
                    movement = 4;
                    return 'r';
                } else if (direction == 1) {
                    movement = 1;
                    return 'r';
                } else if (direction == 2) {
                    return 'f';
                } else if (direction == 3) {
                    movement = 1;
                    return 'l';
                }
            }
        }
        return '0';
    }

    /**
     * Agent's logic when on land
     * @return
     */
    char landTravel() {
		// Uses A* Algorithm to see if we can path to any particular Objectives. 
		// Priority in Descending Order
        char ch = '0';
        if (isDebugging) System.out.println("Searching");
        ch = go_to_object(map, 'k', LAND);
        if (ch == '?' && key > 0) {
            if (isDebugging) System.out.println("Looking for -");
            ch = go_to_object(map, '-', LAND);
        }
        if (ch == '?' && axe > 0) {
            if (isDebugging) System.out.println("Looking for T");
            ch = go_to_object(map, 'T', LAND);
        }
        if (ch == '?' && (stone > 0)) {
            if (isDebugging) System.out.println("Looking for Water");
            ch = go_to_object(map, 'P', LAND);
        }
        if (ch == '?' && treasure > 0) {
            if (isDebugging) System.out.println("Looking for starting spot");
            ch = go_to_object(map, 'M', LAND);
        }
        if (ch == '?' && stone == 0 && raft && axe > 0) {
            if (isDebugging) System.out.println("Looking for Water");
            ch = go_to_object(map, 'G', LAND);
            rafting = true;
        }
        if (ch == '?') {
            prereqs = true;
            return 0;
        }

        return ch;
    }
	/**
     * After we have found our Location to use the Raft, we must then Disembark to our Objective\
	 * We have to modify a Map called VisitedBeyond to complete this Task. Very Similar to VisitedRaft as mentioned Above
	 * Previsited locations are denoted by Y
     * @return if we have found a location to disembark to. 
     */
    char raftDisembarking() {
		// Reset All Existing Priorities
    	int i, j;
        for (i = 0; i < beyondpriority.length; i++) {
            for (j = 0; j < beyondpriority.length; j++) {
                beyondpriority[i][j] = 0;
            }
        }
		// For VisitedBeyond, we have to set all previsited locations to 'E', which is unpathable
		// This is so that we don't land from where we just left
        for (i = 0; i < visitedbeyond.length; i++) {
            for (j = 0; j < visitedbeyond.length; j++) {
                if (visitedbeyond[i][j] == 'Y') {
                    if (originals[i][j] == ' ' || originals[i][j] == 'O') {
                        visitedbeyond[i][j] = 'E';
                    }
                }
            }
        }
		// For all viable departure areas denoted by Q on the map, 
		// we need to see if it can reach the destination we want it to go, after we have disembarked on land
        while (go_to_object(visitedbeyond, 'Q', BEYOND) != '?') {
            int First = beyondcandidate[0];
            int Second = beyondcandidate[1];
            int newPos[] = {First,Second};
			//Set All visited Locations to Unpathable (so we don't go through any Land we have passed)
            for (i = 0; i < visitedbeyond.length; i++) {
                for (j = 0; j < visitedbeyond.length; j++) {
                    if (visitedbeyond[i][j] == 'Y') {
                        visitedbeyond[i][j] = 'Z';
                    }
                }
            }
			// We arrange priorities by sorting Trees as the main focus, followed by other objectives such as Stones, Keys and Doors
            if (can_go_to_object(visitedbeyond, newPos, 'T', BEYOND) == true) { 
                beyondpriority[First][Second] = 4;
            } else if (can_go_to_object(visitedbeyond, newPos, 'o', BEYOND) == true) {
                beyondpriority[First][Second] = 4;
            } else if (can_go_to_object(visitedbeyond, newPos, '-', BEYOND) == true && key > 0) {
                beyondpriority[First][Second] = 3;
            } else if (can_go_to_object(visitedbeyond, newPos, 'M', BEYOND) == true && treasure > 0) {
                beyondpriority[First][Second] = 3;
            } else if (can_go_to_object(visitedbeyond, newPos, '$', BEYOND) == true) {
                beyondpriority[First][Second] = 2;
            } else if (can_go_to_object(visitedbeyond, newPos, '&', BEYOND) == true) {
                beyondpriority[First][Second] = 2;
            } else if (can_go_to_object(visitedbeyond, newPos, ' ', BEYOND) == true) {
				beyondpriority[First][Second] = 1;
			}
			// Reset for next Loop iteration
            for (i = 0; i < visitedbeyond.length; i++) {
                for (j = 0; j < visitedbeyond.length; j++) {
                    if (visitedbeyond[i][j] == 'Z') {
                        visitedbeyond[i][j] = 'Y';
                    }
                }
            }
            visitedbeyond[First][Second] = 'E';
        }
        // When returning back to a Location we have previously visited, the above Code will not be applicable
		// Thus we must have a contingency, set all available candidate locations to %
        for (i = 0; i < visitedbeyond.length; i++) {
            for (j = 0; j < visitedbeyond.length; j++) {
                if (visitedbeyond[i][j] == ' ' && (visitedbeyond[i + 1][j] == 'Y' || visitedbeyond[i - 1][j] == 'Y' || visitedbeyond[i][j + 1] == 'Y' || visitedbeyond[i][j - 1] == 'Y')) {
                    visitedbeyond[i][j] = '%';
                }
            }
        }
		// For each candidate Location, find if it can reach an objective
        while (go_to_object(visitedbeyond, '%', BEYOND) != '?') {
            int First = beyondcandidate[0];
            int Second = beyondcandidate[1];
            int newPos[] = {First,Second};
			// We arrange priorities by sorting Trees as the main focus, followed by other objectives such as Stones, Keys and Doors
            if (go_to_object(visitedbeyond, 'T', BEYOND) != '?') {
                beyondpriority[First][Second] = 4;
            } else if (go_to_object(visitedbeyond, 'T', BEYOND) != '?') {
                beyondpriority[First][Second] = 4;
            } else if (go_to_object(visitedbeyond, '-', BEYOND) != '?' && key > 0) {
                beyondpriority[First][Second] = 3;
            } else if (go_to_object(visitedbeyond, 'M', BEYOND) != '?' && treasure > 0) {
                beyondpriority[First][Second] = 3;
			} else if (go_to_object(visitedbeyond, '$', BEYOND) != '?') {
                beyondpriority[First][Second] = 2;
            } else if (go_to_object(visitedbeyond, ' ', BEYOND) != '?') {
				beyondpriority[First][Second] = 1;
			}		
            visitedbeyond[First][Second] = 'E';
        }
		//Filtering by Priority, if it can't find any Priority 4 Potentials, then it moves to Priority 3 > 2 > 1
		if (BeyondPrioritising(4) == true){
			return 0;
		}
        if (BeyondPrioritising(3) == true){
			return 0;
		}
		if (BeyondPrioritising(2) == true){
			return 0;
		}
		if (BeyondPrioritising(1) == true){
			return 0;
		}
        return 0;
    }

    /**
     * Finds and goes to the first reachable object
     * 
     * @param object - thing that needs to be found
     * @return true if can, false if can't
     */
    boolean can_go_to_object(char[][] map, int[] currPos, char object, int mode) {
        // Finds an instance of an axe that is reachable. Otherwise, do nothing.
        debugSpeed = 5;
        char ch = '?';
        int count = 1; // Counter to track instance of that object
        int[] objLoc = {0,0}; // Init location to start the loop

        // While the axe's location exists
        while (objLoc[0] != -1 || objLoc[1] != -1) {
            objLoc = scan_pos(map, object, count);
            if (objLoc[0] != -1 || objLoc[1] != -1) // Must be a valid location
				if (mode == RAFT || mode == BEYOND){
					ch = a_star(map, objLoc, currPos, direction, ASTAR);
				}
				else {
					ch = a_star(map, objLoc, currPos, direction, GREEDY);
				}
				
            if (ch != '?') break; // If it is a valid move, escape loop
            count++;
        }
        
        if (ch == '?') return false;
        else {
            if (mode == STONE)  candidate = objLoc;
            if (mode == RAFT)   raftcandidate = objLoc;
            if (mode == BEYOND) beyondcandidate = objLoc;
            return true;
        }
    }
    /**
     * Finds and goes to the first reachable object
     * 
     * @param object - thing that needs to be found
     * @return ch of action, '?' if failed
     */
    char go_to_object(char[][] map, char object, int mode) {
        // Finds an instance of an axe that is reachable. Otherwise, do nothing.
        debugSpeed = 5;
        char ch = '?';
        int count = 1; // Counter to track instance of that object
        int[] objLoc = {
            0,
            0
        }; // Init location to start the loop
        int[] currPos = {stateY,stateX};

        // While the axe's location exists
        while (objLoc[0] != -1 || objLoc[1] != -1) {
            objLoc = scan_pos(map, object, count);
            if (objLoc[0] != -1 || objLoc[1] != -1) // Must be a valid location
                if (mode == LAND) ch = a_star(map, objLoc, currPos, direction, ASTAR);
                else ch = a_star(map, objLoc, currPos, direction, GREEDY);
            if (ch != '?') break; // If it is a valid move, escape loop
            count++;
        }
        
        if (ch == '?');
        else {
        	if (mode == BEYOND) beyondcandidate = objLoc;
        }

        return ch;
    }
    /**
     * Finds the position of Xth instance of an object where X is the number of iterations given
     * 
     * @param map - A map of the world
     * @param object - The object to be located
     * @param iter - The number of iterations it must pass before returning an option
     * @return position of the item found at the Xth iteration, a fail position [-1,-1] if not found
     */
    int[] scan_pos(char[][] map, char object, int iter) {
        int[] pos = {-1,
            -1
        }; // Fail instance
        int count = 1;

        for (int y = 0; y < map.length; y++) // only works if its a square map
        {
            for (int x = 0; x < map.length; x++) // square map only
            {
                int[] posUp = {y - 1,x};
                int[] posDown = {y + 1,x};
                int[] posLeft = {y,x - 1};
                int[] posRight = {y,x + 1};

                if (x == 0) posLeft[1] = 0;
                if (y == 0) posUp[0] = 0;
                if (x == map.length - 1) posRight[1] = map.length - 1;
                if (y == map.length - 1) posDown[0] = map.length - 1;

                if (map[y][x] == object) {
                    //if (object == 'T' || object == '-'); // If its a door or tree, do nothing
                    //else 
                    if (count == iter) {
                        pos[0] = y;
                        pos[1] = x;
                        return pos;
                    } else {
                        count++;
                    }
                }
            }
        }
        return pos;
    }
    /**
     * Uses A Star to find an object and chooses a move.
     * For Trees and Doors, it will look for the object and either unlock it or chop it.
     * Heuristic used is the difference between the total of the X and Y distance away from the destination.
     * 
     * Currently cannot move in water.
     * 
     * Note: Most of the coordinates are in [y,x] so be careful
     * Note2: Assumes the tools have already been obtained for doors and trees
     * 
     * @param map - A map with a border. If it is borderless, it crashes.
     * @param dest - The location of the item
     * @param curr - The agent's current location
     * @param dir - The agent's current facing direction
     * @return 'r' or 'f' depending on position, 
     * 'c' or 'u' depending on what object is in front, 
     * '?' if the item cannot be routed to
     */
    char a_star(char map[][], int[] dest, int[] curr, int dir, int parameter) {
        // init
        int depth = 0;
        int initPos[] = {
            curr[0],
            curr[1],
            -1,
            -1,
            depth,
            depth + 1
        }; // {Current Y, Current X, Previous Y, Previous X, Current Depth, Previous Depth}
        boolean itemFound = false;
        char target = map[dest[0]][dest[1]];

        Stack < int[] > moves = new Stack < int[] > (); // {y,x}
        Stack < int[] > visited = new Stack < int[] > (); // {y,x,prevy,prevx}
        PriorityQueue < int[] > notVisited = null;

        if (parameter == ASTAR) {
            notVisited = new PriorityQueue < int[] > (new Comparator < int[] > () {
                public int compare(int[] one, int[] two) {
                    int destOne = Math.abs(dest[1] - one[1]) + Math.abs(dest[0] - one[0]);
                    int destTwo = Math.abs(dest[1] - two[1]) + Math.abs(dest[0] - two[0]);
                    return destOne + one[4] - destTwo - two[4];
                    //return destOne - destTwo;
                }
            }); // {y,x,prevY,prevX} | Also orders by distance from the goal (Distance = |X|+|Y|)    	
        } else // GREEDY
        {
            notVisited = new PriorityQueue < int[] > (new Comparator < int[] > () {
                public int compare(int[] one, int[] two) {
                    int destOne = Math.abs(dest[1] - one[1]) + Math.abs(dest[0] - one[0]);
                    int destTwo = Math.abs(dest[1] - two[1]) + Math.abs(dest[0] - two[0]);
                    return destOne - destTwo;
                    //return destOne - destTwo;
                }
            });
        }

        notVisited.offer(initPos);
        depth++;

        // Pathing to the destination
        while (!notVisited.isEmpty()) {
            int currPos[] = notVisited.poll();
            visited.push(currPos);

            // If we are at the destination, halt loop
            if (currPos[0] == dest[0] &&
                currPos[1] == dest[1]) {
                if (isDebugging) System.out.println("Destination: " + currPos[0] + ", " + currPos[1]);
                itemFound = true;
                break;
            }

            // Add the neighbours of the currPos to notVisited unless (they exist in visited already and they are walls)
            Queue < int[] > neighbours = new LinkedList < int[] > (); //{y,x}
            int posDown[] = {
                (currPos[0] - 1),
                currPos[1]
            };
            int posLeft[] = {
                currPos[0],
                (currPos[1] - 1)
            };
            int posUp[] = {
                (currPos[0] + 1),
                currPos[1]
            };
            int posRight[] = {
                currPos[0],
                (currPos[1] + 1)
            };

            if (posUp[0] >= 0)
                neighbours.add(posUp);
            if (posLeft[1] >= 0)
                neighbours.add(posLeft);
            if (posDown[0] < map.length)
                neighbours.add(posDown);
            if (posRight[1] < map.length)
                neighbours.add(posRight);

            while (!neighbours.isEmpty()) {
                int[] neighbour = neighbours.poll();
                int[] toVisit = {
                    neighbour[0],
                    neighbour[1],
                    currPos[0],
                    currPos[1],
                    depth,
                    currPos[4]
                };
                char charToVisit = map[toVisit[0]][toVisit[1]];

                if (true) {
                    // Prevents backtracking | Does nothing
                    if (containsXY(visited, toVisit));
                    // Add to notVisited if its a Wall or Tree
                    else if (charToVisit == target &&
                        dest[0] == toVisit[0] &&
                        dest[1] == toVisit[1]) {
                        notVisited.offer(toVisit);
                    }
                    // Doesn't register if its a wall either
                    else if (charToVisit == '*' ||
                        charToVisit == '-' ||
                        charToVisit == 'T' ||
                        charToVisit == 'J' ||
						charToVisit == 'H' ||
                        charToVisit == 'G' ||
                        charToVisit == '.' ||
                        charToVisit == 'E' ||
                        charToVisit == 'N' ||
                        charToVisit == 'Z' ||
                        charToVisit == '#' ||
                        charToVisit == '~' //Unreachable Terrains
                    );
                    else {
                        notVisited.offer(toVisit);
                    }
                }
            }
            depth++;
            if (depth > 10000) break;
        }

        // if item cannot be found, return fail, denoted by '?'
        if (!itemFound) {
            return '?';
        }

        // Find path
        if (isDebugging) System.out.println("Final Path:");
        int pos[] = visited.pop();
        int currY = pos[0];
        int currX = pos[1];
        int nextY = pos[2];
        int nextX = pos[3];
        int currD = pos[4];
        int nextD = pos[5];

        int finalInitPos[] = {
            currY,
            currX
        };
        moves.add(finalInitPos);

        while (nextY != -1 || nextX != -1) {
            pos = visited.pop();
            currY = pos[0];
            currX = pos[1];
            currD = pos[4];

            if (isDebugging) System.out.println("Current Depth:" + currD);
            if (isDebugging) System.out.println("Depth:" + nextD);

            // If currPos is next in path, switch to curr and add to path, else do nothing
            if (currY == nextY && currX == nextX && currD == nextD) {
                nextY = pos[2];
                nextX = pos[3];
                nextD = pos[5];

                int[] finalPos = {
                    currY,
                    currX
                };
                moves.add(finalPos);
                if (isDebugging) System.out.println(finalPos[0] + ", " + finalPos[1]);
            }
        }

        // Figure out move
        int nextPos[] = moves.pop();

        // Makes sure the next move isn't the current pos
        while (nextPos[0] == curr[0] && nextPos[1] == curr[1]) {
            nextPos = moves.pop();
        }

        // 1. Turn so its the correct direction. Turn right until correct
        // UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3
        // TODO: Probably can use switch to turn faster
        // Down and character is not facing down
        if (nextPos[0] - 1 == currY &&
            !(dir == 2)) {
            return 'r';
        }
        // Left
        else if (nextPos[1] + 1 == currX &&
            !(dir == 3)) {
            return 'r';
        }
        // Up
        else if (nextPos[0] + 1 == currY &&
            !(dir == 0)) {
            return 'r';
        }
        // Right
        else if (nextPos[1] - 1 == currX &&
            !(dir == 1)) {
            return 'r';
        }
        // Action if next step is dest and target is tree or door
        if (nextPos[0] == dest[0] &&
            nextPos[1] == dest[1] &&
            map[nextPos[0]][nextPos[1]] != ' ') {
            // It should definitely be able to do these actions. As such, it will continue exploring new space
            // One thing to note, it needs to re explore as soon as the door/tree is removed as their map updates but not ours.
            doneExploring = false;
            if (target == '-') {
                key--;
                return 'u';
            } else if (target == 'T') {
                raft = true;
                return 'c';
            } else if (target == 'P') {
                stone--;
                return 'f';
            } else if (target == '$') {
                treasure++;
                return 'f';
            } else if (target == 'B') {
                int i, j;
                for (i = 0; i < visitedbeyond.length; i++) {
                    for (j = 0; j < visitedbeyond.length; j++) {
                        if (visitedbeyond[i][j] == 'E' && (visitedbeyond[i + 1][j] == 'Y' || visitedbeyond[i - 1][j] == 'Y' || visitedbeyond[i][j + 1] == 'Y' || visitedbeyond[i][j - 1] == 'Y')) {
                            visitedbeyond[i][j] = 'Q';
                        }
                        if (visitedbeyond[i][j] == 'E') {
                            visitedbeyond[i][j] = '&';
                        }
                    }
                }
                ready = false;
                raft = false;
                raftsearch = false;
                rafting = false;
                doneExploring = false;
                return 'f';
            } else if (target == 'G') {
				int i,j;
				Embark = false;
                doneExploring = false;
                rafting = true;
				for (i = 0; i < visitedraft.length; i++) {
					for (j = 0; j < visitedraft.length; j++) {
						if (visitedraft[i][j] != 'N') {
							visitedraft[i][j] = map[i][j];
						}
					}
				}
                return 'f';
            }
        }

        // 2. If its the correct direction, move forward
        return 'f';

        // 3. EXTRA: Use resources if you can't move forward
        // Requires an edit so it can do that
    }

    /**
     * movements to see if an element exists within a stack
     * 
     * @param stack - The stack(list) to be tested against
     * @param test - the element(position) to be tested
     * @return true if it exists, false if it doesn't
     */
    boolean containsXY(Stack < int[] > stack, int[] test) {
        for (int[] e: stack) {
            if (e[0] == test[0] && e[1] == test[1])
                return true;
        }

        return false;
    }
	/** 
	 *
	 * Finds all Priorities for a certain Level for Rafting Objectives
	 * @param prio - The degree of priority
	 * @return true if the priority has been found
	 */
	boolean BeyondPrioritising(int prio){
		int i, j;
		for (i = 0; i < beyondpriority.length; i++) {
			for (j = 0; j < beyondpriority.length; j++) {
				if (beyondpriority[i][j] == prio) { 
					beyondpriority[i][j] = 0;
					visitedbeyond[i][j] = 'B';
					ready = true;
					return true;
				}
			}
		}
		return false;
	}
	/** 
	 *
	 * Finds all Priorities for a certain Level for Rafting Objectives
	 * @param prio - The degree of priority
	 * @return true if the priority has been found
	 */
	boolean RaftPrioritising(int prio) {
        int i, j;
		for (i = 0; i < raftpriority.length; i++) {
			for (j = 0; j < raftpriority.length; j++) {
				if (raftpriority[i][j] == prio) { 
					raftpriority[i][j] = 0;
					map[i][j] = 'G';
					prereqs = false;
					return true;
				}
			}
		}
        return false;
    }
	/** 
	 *
	 * Finds all Priorities for a certain Level for Stone Placing Objectives
	 * @param prio - The degree of priority
	 * @return true if the priority has been found
	 */
    boolean Prioritising(int prio) {
        int i, j;
        for (i = 0; i < priority.length; i++) {
            for (j = 0; j < priority.length; j++) {
                if (priority[i][j] == prio) { 
                    if (stone > 0) {
                        priority[i][j] = 0;
                        map[i][j] = 'P';
                        originals[i][j] = 'P';
                        prereqs = false;
                        return true;
                    }
                }
            }
        }
        return false;
    }
	/** 
	 *
	 * We have to connect all 'S' on the Visitedraft map, to test if we can reach the location with one raft only
	 * @return True if we have finished connecting, false if haven't
	 * 
	 */
	boolean Connected(){
		int i,j;
		for (i = 0; i < visitedraft.length; i++) {
			for (j = 0; j < visitedraft.length; j++) {
				if (visitedraft[i][j] == '&'){
					if (visitedraft[i+1][j] == 'S' || visitedraft[i-1][j] == 'S' || visitedraft[i][j+1] == 'S' || visitedraft[i][j-1] == 'S') {
						visitedraft[i][j] = 'S';
						return false;
					}
				}
			}
		}
		return true; 
	}
	/** 
	 *
	 * Updates if we have found a new Tool at a specific direction
	 * @param directions - it is dependent on which way we are moving. 
	 * 
	 */
    void VariableUpdate(char directions) {
        if (directions == 'k') {
            key++;
        }
        if (directions == 'a') {
            axe++;
        }
        if (directions == '$') {
            treasure++;
        }
        if (directions == 'o') {
            stone++;
        }
        XStack.push(stateX);
        YStack.push(stateY);
    }
	/** 
	 *
	 * Uses Cantor Pairing Function to pair multiple stones together, then cycle through to find originating location
	 * @param prio - The level of priority of the objective found 
	 * @param currVal - The current Cantor result from Pairing the X,Y variables of the Objective
	 */
    void Cantor(int prio, int currVal) {
        while (CantorX[currVal] != 0) {
            placeholder = currVal;
            UseX = CantorX[currVal];
            UseY = CantorY[currVal];
            currVal = (((UseX + UseY) * (UseX + UseY + 1)) / 2) + UseY;
        }
        priority[UseX][UseY] = prio;
        CantorX[placeholder] = 0;
        CantorY[placeholder] = 0;
    }
	/** 
	 *
	 * Checks if it has found a location to find a new stone
	 * @param First - X Loc of Object
	 * @param Second - Y Loc of Object
	 */
	
    void foundStone(int First, int Second) {
        int i, j;
        priority[First][Second] = 5;
        for (i = 0; i < visitedwater.length; i++) {
            for (j = 0; j < visitedwater.length; j++) {
                if (visitedwater[i][j] == 'E') {
                    visitedwater[First][Second] = '#';
                }
            }
        }
    }
    void print_view(char view[][]) {
        int i, j;

        System.out.println("\n+-----+");
        for (i = 0; i < 5; i++) {
            System.out.print("|");
            for (j = 0; j < 5; j++) {
                if ((i == 2) && (j == 2)) {
                    System.out.print('^');
                } else {
                    System.out.print(view[i][j]);
                }
            }
            System.out.println("|");
        }
        System.out.println("+-----+");
    }
    /** 
	 *
	 * Used for Printing the Entire Map
	 * @param view - Which Map we are printing
	 * 
	 */
    void print_view_map(char view[][]) {
        int i, j;
        System.out.println("\n+--------------------------------------------------+");
        for (i = 0; i < view.length; i++) {
            System.out.print("|");
            for (j = 0; j < view.length; j++) {
                if ((view[i][j] == 0)) {
                    System.out.print(' ');
                } else {
                    System.out.print(view[i][j]);
                }
            }
            System.out.println("|");
        }
        System.out.println("+--------------------------------------------------+");
    }

    public static void main(String[] args) {
        InputStream in = null;
        OutputStream out = null;
        Socket socket = null;
        Agent agent = new Agent();
        char view[][] = new char[5][5];
        char action = 'F';
        int port;
        int ch;
        int chars;
        int i, j;
        int initialX = stateX; 
        int initialY = stateY;
        boolean blocked = false;	 // checks if there is an obstruction ahead
        char last = 'f'; 			 // most recent action
        int prevdirection = 2; 		 // previous facing direction
		
		//Prefill Empty Map with Unpathable Terrain for Unexplored Areas ('N')
        if (startFlag == 0) {
            startFlag++;
            for (i = 0; i < map.length; i++) {
                for (j = 0; j < map.length; j++) {
					visitedbeyond[i][j] = 'N';
                    visitedwater[i][j] = 'N';
					visitedraft[i][j] = 'N';
                }
            }
        }

        if (args.length < 2) {
            System.out.println("Usage: java Agent -p <port>\n");
            System.exit(-1);
        }

        port = Integer.parseInt(args[1]);
        try { // open socket to Game Engine
            socket = new Socket("localhost", port); in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            System.out.println("Could not bind to port: " + port);
            System.exit(-1);
        }

        try { // scan 5-by-5 window around current location
            while (true) {
                for (i = 0; i < 5; i++) {
                    for (j = 0; j < 5; j++) {
                        if (!((i == 2) && (j == 2))) {
                            ch = in .read();
                            if (ch == -1) {
                                System.exit(-1);
                            }
                            view[i][j] = (char) ch;
                            int currentX = 0;
                            int currentY = 0;
                            if ((last == 'f') || (last == 'F')) {
                                if (direction == 2) {
                                    currentX = initialX + j;
                                    currentY = initialY + i;
                                } else if (direction == 3) {
                                    //adjusting for rotation
                                    currentX = initialX + (4 - i);
                                    currentY = initialY + j;
                                } else if (direction == 0) {
                                    //adjusting for rotation
                                    currentX = initialX + (4 - j);
                                    currentY = initialY + (4 - i);
                                } else if (direction == 1) {
                                    //adjusting for rotation
                                    currentX = initialX + i;
                                    currentY = initialY + (4 - j);
                                }
                                currentY = mapsize - currentY;
                                currentX = mapsize - currentX;
                                if (map[currentY][currentX] == 0) {
                                    map[currentY][currentX] = (char) ch;
                                    originals[currentY][currentX] = (char) ch;
                                    visitedwater[currentY][currentX] = (char) ch;
                                    visitedraft[currentY][currentX] = (char) ch;
                                    visitedbeyond[currentY][currentX] = (char) ch;
                                }
                            }
                        }
                    }
                }
                if (raftsearch == true && AlreadyBeen == 1) {
                    for (i = 0; i < map.length; i++) {
                        for (j = 0; j < map.length; j++) {
                            if ((originals[i][j] != '~') && (visitedbeyond[i][j] == 'Y')) {
                                visitedbeyond[i][j] = 'E';
                            }
                        }
                    }
                    AlreadyBeen++;
                }

                // clear map of X (will have to modify when moving across water)
                for (i = 0; i < map.length; i++) {
                    for (j = 0; j < map.length; j++) {
                        if ((map[i][j] == 'X')) {

                            if (originals[i][j] != '~' && originals[i][j] != 'P' && originals[i][j] != 'G' && originals[i][j] != 'O' && originals[i][j] != '*') {
                                map[i][j] = ' ';
                            } else if (originals[i][j] == 'P') {
                                map[i][j] = 'O';
                                originals[i][j] = 'O';
                                doneExploring = false;
                            } else if (originals[i][j] == 'G') {
                                map[i][j] = '~';
                                originals[i][j] = '~';
                                doneExploring = false;
                            } else {
                                map[i][j] = originals[i][j];
                            }

                        }
                    }
                }
                map[initialsY][initialsX] = 'M';
                visitedwater[initialsY][initialsX] = 'M';
                visitedraft[initialsY][initialsX] = 'M';
                visitedbeyond[initialsY][initialsX] = 'M';

                map[mapsize - 2 - initialY][mapsize - 2 - initialX] = 'X';
                visitedmap[mapsize - 2 - initialY][mapsize - 2 - initialX] = 'X'; 			//records visited locations
                visitedwater[mapsize - 2 - initialY][mapsize - 2 - initialX] = 'X';			//records visited Water Areas
                visitedraft[mapsize - 2 - initialY][mapsize - 2 - initialX] = 'X';			//records visited Raft Embark Locations
                visitedbeyond[mapsize - 2 - initialY][mapsize - 2 - initialX] = 'Y';		//records visited Raft Areas
                stateX = mapsize - 2 - initialX;											//records current X 
                stateY = mapsize - 2 - initialY;											//records current Y
                if ((view[1][2] == '*') || (view[1][2] == '-') || (view[1][2] == 'T')) {    //checks if there is a blockage ahead
                    blocked = true;
                } else {
                    blocked = false;
                }
                //agent.print_view(view); // current 5x5 view, comment out before submission
				//agent.print_view_map(map); // world map, comment out before submission
                action = agent.get_action(view);
                if ((action == 'F') || (action == 'f')) {
                    if (blocked == false) {
                        if (direction == 0) {
                            initialY = initialY + 1;
                        } else if (direction == 1) {
                            initialX = initialX - 1;
                        } else if (direction == 2) {
                            initialY = initialY - 1;
                        } else if (direction == 3) {
                            initialX = initialX + 1;
                        }
                    }
                } else if ((action == 'L') || (action == 'l')) { 	//finds and updates which direction the agent is facing
                    if ((last != 'l') || (last != 'L')) {
                        prevdirection = direction;
                    }
                    direction--;
                    if (direction < 0) {
                        direction = 3;
                    }
                } else if ((action == 'R') || (action == 'r')) {	//finds and updates which direction the agent is facing
                    if ((last != 'r') || (last != 'R')) {
                        prevdirection = direction;
                    }
                    prevdirection = direction;
                    direction++;
                    if (direction > 3) {
                        direction = 0;
                    }
                }
				//records last direction changing action
                if (action == 'r' || action == 'R' || action == 'L' || action == 'l') {
                    lastdirection = action;
                }
                last = action;
                out.write(action);
            }
        } catch (IOException e) {
            System.out.println("Lost connection to port: " + port);
            System.out.println("Time taken = " + (System.currentTimeMillis() - timeStart) + "ms");
            System.exit(-1);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }
}