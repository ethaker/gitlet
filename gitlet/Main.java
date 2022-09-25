package gitlet;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
 * Main class handles user command line input/calls the according
 * methods.
 *  @author Eesha Thaker
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException, ParseException {
        Repo r = new Repo();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        switch(args[0]) {
            case "init":
                r.init();
                break;
            case "add":
                r.add(args[1]);
                break;
            case "checkout":
                if (args.length == 3) {
                    //handles checkout -- <fileName> format
                    r.checkout(args[2]);
                    break;
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        break;
                    }
                    r.checkoutID(args[1], args[3]);
                    break;
                } else if (args.length == 2) {
                    r.checkoutBranch(args[1]);
                    break;
                }
            case "log":
                r.log();
                break;
            case "status":
                r.status();
                break;
            case "rm":
                r.rm(args[1]);
                break;
            case "find":
                r.find(args[1]);
                break;
            case "global-log":
                r.globalLog();
                break;
            case "branch":
                r.branch(args[1]);
                break;
            case "rm-branch":
                r.rm_branch(args[1]);
                break;
            case "reset":
                r.reset(args[1]);
                break;
            case "commit":
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                r.commit(args[1]);
                break;
            case "merge":
                r.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
