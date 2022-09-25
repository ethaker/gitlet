package gitlet;

import org.antlr.v4.runtime.tree.Tree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Class that represents the Gitlet repository
 * within the current working directory. Gitlet repository contains
 * a staging area, as well as a master commit branch that tracks
 * all commits. Both of these are represented as files.
 *
 * @author Eesha Thaker
 */

public class Repo implements java.io.Serializable {

    /** Pointer to master branch. */
    private Commit master;

    /** File storing blobs that have been committed;
     * copies of files that have been committed (referenced from
     * commit objects through their hashes). */
    private File committedBlobs;

    /** Pointer to current commit */
    private Commit HEAD;

    /** Current working directory, where gitlet directory will
     * be initialized. */
    private File currentWorkingDir = new File(System.getProperty("user.dir"));

    /** TreeMap used to store files that are staged for removal,
     * maps file names to their blob references. */
    private TreeMap<String, String> stageForRemoval;

    /** File used to persist stageForRemoval. */
    private File stageForRemovalStorage;

    /** File where all commits objects are stored. */
    private File commits;

    /** File referencing gitletDirectory. */
    private File gitletDir;

    /** Variable used to generate treeMap for new commits,
     * maps file names to their blob references (which version
     * of the file is being committed). */
    private TreeMap<String, String> newBlobReferences;

    /** File used to persist treeMap. */
    private File newBlobReferencesStorage;

    /** File used to persist HEAD pointer. */
    private File headPointer;

    /** TreeMap mapping commit hashIDs to references
     * to those commit objects
     */
    private TreeMap<String, Commit> allCommits;

    /** TreeMap mapping the names of branches, and the
     * commit object that they point to. */
     private TreeMap<String, Commit> allBranches;

     /** File used to persist allBranches TreeMap. */
    private File allBranchesFile;

    /** File used to persist allCommits TreeMap */
    private File allCommitsFile;

    /** Initializes files within the .gitlet repository within CWD. */
    public Repo() {
        this.gitletDir = new File(currentWorkingDir+"/.gitlet");
        this.commits = new File(gitletDir+"/commits");
        this.committedBlobs = new File(gitletDir+"/committedBlobs");
        this.newBlobReferencesStorage = new File(gitletDir+"/newBlobReferences");
        this.newBlobReferences = new TreeMap<>();
        this.headPointer = new File(gitletDir+"/headPointer");
        this.allCommits = new TreeMap<>();
        this.allCommitsFile = new File(gitletDir+"/allCommits");
        this.stageForRemoval = new TreeMap<>();
        this.stageForRemovalStorage = new File(gitletDir+"/stageForRemoval");
        this.allBranches = new TreeMap<>();
        this.allBranchesFile = new File(gitletDir+"/allBranches");
    }

    /** GOAL: Create gitlet directory in CWD.
     * (Gitlet directory should be hidden file in CWD)
     * Creates the first commit object with no parent commits,
     * given commit/timestamp, sets HEAD to current commit and
     * adds reference (SHA-1 ID) of commit to master branch. */
    public void init() {
        //Handles exception: should not create .gitlet directory if already exists in CWD
        String[] currFiles = currentWorkingDir.list();
        for (String f: currFiles) {
            if (f.equals(".gitlet")) {
                System.out.println("A Gitlet version-control system " +
                        "already exists in the current directory.");
                return;
            }
        }

        //Create gitlet directory if doesn't already exist
        gitletDir.mkdir();
        //subdirectories within gitletDirectory
        commits.mkdir();
        committedBlobs.mkdir();

        //create first commit
        Commit commit0 = new Commit("initial commit",
                Date.from(Instant.EPOCH), null, null);
        commit0.setCurrID();
        assert(commit0.blobReferences != null);

        //Set head and master pointers
        HEAD = commit0;
        master = HEAD;
        master.setName("master");
        allBranches.put("master", HEAD);

        //add commit0 to treeMap
        allCommits.put(commit0.getCurrID(), commit0);

        //create first commit
        Utils.writeContents(new File(commits+"/commit0"), Utils.serialize(master));
        Utils.writeContents(headPointer, Utils.serialize(HEAD));
        Utils.writeContents(allCommitsFile, Utils.serialize(allCommits));
        Utils.writeContents(allBranchesFile, Utils.serialize(allBranches));
        Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));
        Utils.writeContents(stageForRemovalStorage, Utils.serialize(stageForRemoval));
    }

    /** GOAL: Serialize files (make copy of contents of files) to create
     * a BLOB, and add BLOBS to staging area.
     * TreeMap currentCommitFiles tracks files in most recent commit,
     * mapping file names to hash values (tracking contents of files).
     * If TreeMap doesn't contain file:
     *     Add file name + hash to tree map & serialize file and add to
     *     staging area
     * If TreeMap contains file:
     *      Check if file has been updated (contain hash values). If updated,
     *      change hash value & serialize file and add to staging area
     *      If not updated, do nothing
     * @param addedFile
     */
    @SuppressWarnings("unchecked")
    public void add(String addedFile) {
        //get the path of the file to add (file should be in CWD), given its name
        File filetoStage = new File(currentWorkingDir.getPath()+"/"+addedFile);
        if (!filetoStage.exists()) {
            System.out.println("File does not exist");
            return;

        } else {
            //If file is in removed files directory, simply un-remove it, and don't add it
            //to staging area
            stageForRemoval = Utils.readObject(stageForRemovalStorage, TreeMap.class);
            Set<String> removedFiles = stageForRemoval.keySet();
            for (String file : removedFiles) {
                if (file.equals(addedFile)) {
                    stageForRemoval.remove(file);
                    Utils.writeContents(stageForRemovalStorage, Utils.serialize(stageForRemoval));
                    return;
                }
            }


            Blob addedBlob = new Blob(filetoStage);
            String blobHash = addedBlob.getHash();
            HEAD = Utils.readObject(headPointer, Commit.class);
            newBlobReferences = Utils.readObject(newBlobReferencesStorage, TreeMap.class);

            if (!HEAD.blobReferences.containsKey(addedFile) && !newBlobReferences.containsKey(addedFile)) {
                //serialize the file and add it to staging area TreeMap
                newBlobReferences.put(addedFile, blobHash);
                Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));

            } else {
                if (HEAD.blobReferences.containsKey(addedFile)
                        && !HEAD.blobReferences.get(addedFile).equals(blobHash)) {
                    newBlobReferences.put(addedFile, blobHash);
                    Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));
                }
                else if (!HEAD.blobReferences.containsValue(blobHash) && !newBlobReferences.containsValue(blobHash)) {
                    //serialize the file and add it to staging area TreeMap
                    newBlobReferences.put(addedFile, blobHash);
                    Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));

                } else if (HEAD.blobReferences.containsValue(blobHash) && newBlobReferences.containsValue(blobHash)) {
                    //do nothing, don't add file to staging area
               }
            }
        }
    }

    /** If a file is in staging area, removes it from staging area,
     * and deletes it from CWD (if it is tracked in current commit).
     * @param removedFile
     * */
    @SuppressWarnings("unchecked")
    public void rm(String removedFile) {
        //If file is not being tracked by current commit, don't remove it (exception case)
        HEAD = Utils.readObject(headPointer, Commit.class);
        newBlobReferences = Utils.readObject(newBlobReferencesStorage, TreeMap.class);
        if (!HEAD.blobReferences.containsKey(removedFile) && !newBlobReferences.containsKey(removedFile)) {
           System.out.println("No reason to remove the file.");
        } else {
            //If file is being tracked by current commit:
            File cwdFile = new File(currentWorkingDir.getPath()+"/"+removedFile);
            boolean exists = cwdFile.exists();

            //stage file for removal (add it to removal staging directory) IF FILE WAS TRACKED IN PREVIOUS COMMIT
            if (HEAD.blobReferences.containsKey(removedFile)) {
                stageForRemoval.put(removedFile, HEAD.blobReferences.get(removedFile));
                Utils.writeContents(stageForRemovalStorage, Utils.serialize(stageForRemoval));
                //Remove it from CWD, only IF IT WAS TRACKED IN PREVIOUS COMMIT
                cwdFile.delete();
                //Make sure that file is not tracked by next commit (since it has been deleted)
                HEAD.blobReferences.remove(removedFile);
            }

        }

        //If file exists in staging area, remove it from staging area
        newBlobReferences.remove(removedFile);
        HEAD.blobReferences.remove(removedFile);
        Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));
        Utils.writeContents(headPointer, Utils.serialize(HEAD));
    }

    /** Add a new commit object to the commit tree:
     * Clone previous commit, adjust parameters:
     *     timeStamp = new timeStamp
     *     message = new message
     *     parentHash1 = HEAD.currID (hash value of prev commit)
     * For all files in staging directory:
     *      Move files to committedBlobs directory (persistence)
     *      Delete these files (clear staging directory, will not harm CWD
     *      since all files in staging directory are copies of original files.
     * Adjust head/master pointers.
     * @param message
     */
    @SuppressWarnings("unchecked")
    public void commit(String message) throws IOException {
        Pattern p = Pattern.compile(".+");
        Matcher m = p.matcher(message);

        if (!m.matches()) {
            System.out.println("Please enter a commit message.");
        }

        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        HEAD = Utils.readObject(headPointer, Commit.class);
        newBlobReferences = Utils.readObject(newBlobReferencesStorage, TreeMap.class);
        stageForRemoval = Utils.readObject(stageForRemovalStorage, TreeMap.class);
        if (newBlobReferences.keySet().size() == 0 && stageForRemoval.keySet().size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        //delete all files that are staged for removal
        for (String f : stageForRemoval.keySet()) {
            newBlobReferences.remove(f);
        }
        stageForRemoval = new TreeMap<String, String>();
        Utils.writeContents(stageForRemovalStorage, Utils.serialize(stageForRemoval));

        //set fields of new commit object
        Commit next = Commit.copy(HEAD);
        next.setTimeStamp(new Date());
        next.setCommitMessage(message);
        next.setParentHash1(HEAD.getCurrID());
        next.setCurrID();
        File newCommit = new File(commits.getPath()+"/"+next.getCurrID());
        Utils.writeObject(newCommit, next);
        next.blobReferences = HEAD.blobReferences;
        for (String file : newBlobReferences.keySet()) {
            if (!HEAD.blobReferences.containsKey(file)) {
                next.blobReferences.put(file, newBlobReferences.get(file));
            } else if (HEAD.blobReferences.containsKey(file)) {
                next.blobReferences.put(file, newBlobReferences.get(file));
            }
        }

        //Move blobs from staging directory to committedBlobs directory
        Set<String> stagedForAddition = newBlobReferences.keySet();
        for (String stagedFile : stagedForAddition) {
            File addFile = new File(currentWorkingDir.getPath()+"/"+stagedFile);
            Blob persistingBlob = new Blob(addFile);
            File persistedBlobFile = new File(committedBlobs.getPath()+"/"+persistingBlob.getHash());

            File[] currentBlobs = committedBlobs.listFiles();
            boolean exists = false;
            for (File blob : currentBlobs) {
                if (blob.getName().equals(persistedBlobFile.getName())) {
                    exists = true;
                }
            }

            if (exists == false) {
                Utils.writeContents(persistedBlobFile, Utils.readContentsAsString(addFile));
            }
        }

        //re-adjust head/branch pointers:
        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        Commit currBranch = allBranches.get(HEAD.getName());

        String name = HEAD.getName();
        //Move the head pointer
        HEAD = next;
        HEAD.setName(name);
        Utils.writeObject(headPointer, HEAD);

        //Move the branch head of whatever HEAD is set to (HEAD points to current branch)
        currBranch = HEAD;
        allBranches.replace(HEAD.getName(), HEAD);
        Utils.writeContents(allBranchesFile, Utils.serialize(allBranches));

        //reset TreeMap with staged commits
        newBlobReferences = new TreeMap<>();
        Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));

        //How to set parentHash2 of commit object?

        allCommits.put(HEAD.getCurrID(), HEAD);
        Utils.writeContents(allCommitsFile, Utils.serialize(allCommits));

    }

    /** Makes copy of file with name fileName from HEAD commit,
     * and moves it to CWD. Should only be accessing files from
     * commit that HEAD points to! (Previous commit = HEAD)
     * If file already exists in CWD, it is
     * overwritten by copied file.
     * If file does not exist in HEAD commit, throw error. */
    public void checkout(String fileName) {
        //throw error if file isn't found in iteration
        //get blobReferences from HEAD object, which are persisted in memory
        HEAD = Utils.readObject(headPointer, Commit.class);
        checkoutID(HEAD.getCurrID(), fileName);
    }

    /** Makes copy of file with name fileName from given commit,
     * and moves it to CWD. Should only be accessing files from
     * commit that given commit points to!
     * If file already exists in CWD, it is
     * overwritten by copied file.
     * If file does not exist in given commit, throw error. */
    @SuppressWarnings("unchecked")
    public void checkoutID(String commitID, String fileName) {
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        Commit thisCommit = null;
        for (String c : allCommits.keySet()) {
            if (c.substring(0, 8).equals(commitID.substring(0, 8))) {
                thisCommit = allCommits.get(c);
                break;
            }
        }

        if (thisCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        } else if (!thisCommit.blobReferences.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
                //Make a copy of file, overwrite version currently in CWD
                File copyFile = new File(currentWorkingDir.getPath()+"/"+ fileName);
                String blobHash = thisCommit.blobReferences.get(fileName);
                String fileContents = Utils.readContentsAsString(
                        new File(committedBlobs.getPath()+"/"+blobHash));
                Utils.writeContents(copyFile, fileContents);
            }

        //set HEAD pointer to specified commit
        HEAD = thisCommit;
        Utils.writeContents(headPointer, Utils.serialize(HEAD));
    }

    /** Checks out all commits at head of branch branchName.
     * Sets HEAD pointer to branchName commit.
     * Deletes any files that are tracked in HEAD commit but
     * not branchName commit.
     * Clear staging area, unless branchName is HEAD.
     * REMINDERS: master = current branch. HEAD = most recent commit. */
    @SuppressWarnings("unchecked")
    public void checkoutBranch(String branchName) {
        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        HEAD = Utils.readObject(headPointer, Commit.class);

        //if no such branch exists, throw error case
        if (!allBranches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }

        //If the contents at the HEAD branch match those at branchName, throw an error case
        if (HEAD.getName().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        //checks if there is file in CWD that isn't tracked in current branch
        if (getUntrackedFiles(branchName)) {
            System.out.println("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");
            return;
        }

        Commit checkoutCommit = allBranches.get(branchName);
        //Delete (rm) any files that are tracked in HEAD commit but not branchName commit
        for (String blob : HEAD.blobReferences.keySet()) {
            if (!checkoutCommit.blobReferences.containsKey(blob)) {
                File toRemove = new File(currentWorkingDir.getPath() + "/" + blob);
                Utils.restrictedDelete(toRemove);
            }
        }

        //If there is a file in the CWD that isn't tracked in current branch, throw error:
        //Gives list of all files in branch
        ArrayList<String> branchFiles = new ArrayList<String>();
        HEAD = checkoutCommit;
        Commit currCommit = HEAD;
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        while (currCommit.getParentHash1() != null) {
            for (String blob : currCommit.blobReferences.keySet()) {
                branchFiles.add(blob);
            }
            currCommit = allCommits.get(currCommit.getParentHash1());
        }

        //Check out all blobs at current branch/commit
        for (String blob : checkoutCommit.blobReferences.keySet()) {
            checkoutID(checkoutCommit.getCurrID(), blob);
        }

        //Set head pointer to current branch
        HEAD = checkoutCommit;
        HEAD.setName(branchName);
        Utils.writeContents(headPointer, Utils.serialize(HEAD));

        //clear staging area
        newBlobReferences = new TreeMap<String, String>();
        Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));

    }

    /** Display information about each commit, starting from HEAD.
     * Prints the following information:
     *      ===
     *      commit <commit's hashID>
     *      commit's timeStamp, in PST
     *      commit's message
     */
    @SuppressWarnings("unchecked")
    public void log() throws ParseException {
        HEAD = Utils.readObject(headPointer, Commit.class);
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        Commit currCommit = HEAD;

        SimpleDateFormat myDate = new SimpleDateFormat("Z");
        String timeOffset = myDate.format(currCommit.getTimeStamp());

        while (currCommit.getParentHash1() != null) {
            //special case for merge commits
            if (currCommit.getParentHash2() != null) {
                System.out.println("===");
                System.out.println("commit " + currCommit.getCurrID());

                String mergeString = "Merge: " + currCommit.getParentHash1().substring(0, 7)
                        + " " + currCommit.getParentHash2().substring(0, 7);
                System.out.println(mergeString);

                //Format timeStamp correctly to match desired output
                System.out.println("Date: " + String.format("%1$ta %1$tb %1$td %1$tT %1$tY ", currCommit.getTimeStamp()) + timeOffset);

                System.out.println(currCommit.getCommitMessage());
                System.out.println();
            } else {
                System.out.println("===");
                System.out.println("commit " + currCommit.getCurrID());

                //Format timeStamp correctly to match desired output
                System.out.println("Date: " + String.format("%1$ta %1$tb %1$td %1$tT %1$tY ", currCommit.getTimeStamp()) + timeOffset);

                System.out.println(currCommit.getCommitMessage());
                System.out.println();
            }

            currCommit = allCommits.get(currCommit.getParentHash1());
        }

        //prints information of commit0
        System.out.println("===");
        System.out.println("commit " + currCommit.getCurrID());
        System.out.println("Date: Wed Dec 31 16:00:00 1969 -0800");
        System.out.println(currCommit.getCommitMessage());
    }

    /** Displays metadata about gitlet subdirectories:
     * Branches (with * next to current branch)
     * Files in staging area (staged for addition)
     * Files in removal area (staged for removal)
     * Files that have been modified but not committed
     *      Modifications made to those files (deleted or modified)
     * Untracked files: files that are in CWD that aren't anywhere in gitlet subdirectory
     * (If treemap doesn't contain files in CWD, they are untracked)
     * */
    @SuppressWarnings("unchecked")
    public void status() {
        String[] cwd = currentWorkingDir.list();
        ArrayList<String> cwdFiles = new ArrayList<String>();
        for (int i = 0; i < cwd.length; i++) {
            cwdFiles.add(cwd[i]);
        }
        if (!cwdFiles.contains(".gitlet")) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        //Print out all the branches, with a * next to the current branch
        System.out.println("=== Branches ===");

        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        HEAD = Utils.readObject(headPointer, Commit.class);
        String currBranch = HEAD.getName();
        System.out.println("*" + currBranch);

        Set<String> branchNames = allBranches.keySet();
        for (String branch : branchNames) {
            if (!branch.equals(currBranch)) {
                System.out.println(branch);
            }
        }
        System.out.println();

        //Print out all files staged for addition
        System.out.println("=== Staged Files ===");
        newBlobReferences = Utils.readObject(newBlobReferencesStorage, TreeMap.class);
        for (String file : newBlobReferences.keySet()) {
            System.out.println(file);
        }
        System.out.println();

        //Print out all files staged for removal
        System.out.println("=== Removed Files ===");
        stageForRemoval = Utils.readObject(stageForRemovalStorage, TreeMap.class);
        Set<String> removedFiles = stageForRemoval.keySet();
        for (String f : removedFiles) {
            System.out.println(f);
        }
        System.out.println();

        //Print out all files that have been modified and not staged
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        //Print out all files that are untracked
        //files in CWD that are not tracked in current branch or staging area
        //files in CWD that are staged for removal
        System.out.println("=== Untracked Files ===");
        System.out.println();

        System.out.println();
    }

    /** Prints IDs of all commits that have the given commit message,
     * printing IDs of each commit on a separate line */
    public void find(String message) {
        File[] allCommits = commits.listFiles();
        boolean exists = false;
        for (File commit : allCommits) {
            Commit thisCommit = Utils.readObject(commit, Commit.class);
            if (thisCommit.getCommitMessage().equals(message)) {
                exists = true;
                System.out.println(thisCommit.getCurrID());
            }
        }
        if (exists == false) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays metadata (similar to that in log) about all commits ever made.
     * Order of commits does not matter-- simply need to display information
     * about all commits in commits directory. */
    @SuppressWarnings("unchecked")
    public void globalLog() throws ParseException {
        //gives the names of all files in commits-- commits are named by their currID values
        File[] allCommits = commits.listFiles();
        for (File commitFile : allCommits) {
            Commit thisCommit = Utils.readObject(commitFile, Commit.class);

            System.out.println("===");
            System.out.println("commit " + thisCommit.getCurrID());

            //Format timeStamp correctly to match desired output
            SimpleDateFormat myDate = new SimpleDateFormat("Z");
            String timeOffset = myDate.format(thisCommit.getTimeStamp());
            System.out.println("Date: " + String.format("%1$ta %1$tb %1$td %1$tT %1$tY ", thisCommit.getTimeStamp()) + timeOffset);

            System.out.println(thisCommit.getCommitMessage());
            System.out.println();

        }
    }

    /** Create a new branch with the given name and point it at head node.
     * Recall: a branch is pointer to a commit object. */
    @SuppressWarnings("unchecked")
    public void branch(String branchName) {
        HEAD = Utils.readObject(headPointer, Commit.class);
        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        Commit newBranchPointer = HEAD;
        newBranchPointer.setName(branchName);

        if (allBranches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            allBranches.put(branchName, newBranchPointer);
            Utils.writeContents(allBranchesFile, Utils.serialize(allBranches));
        }
    }

    /** Deletes the branch with the given name.
     * Does so by removing persisting references to branch
     * (Branches are stored in memory through references to TreeMap that
     * is stored in memory). */
    @SuppressWarnings("unchecked")
    public void rm_branch(String branchName) {
        HEAD = Utils.readObject(headPointer, Commit.class);
        if (branchName.equals(HEAD.getName())) {
            System.out.println("Cannot remove the current branch.");
        }
        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        if (!allBranches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        allBranches.remove(branchName);
        Utils.writeContents(allBranchesFile, Utils.serialize(allBranches));
    }

    /** Checks out all files at given commit, given with
     * commitID. Also moves HEAD to commitID commit,
     * sets the current branch to current commit.
     * @param commitID
     */
    @SuppressWarnings("unchecked")
    public void reset(String commitID) {
        //Move the HEAD pointer to the current commit
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        Commit thisCommit = allCommits.get(commitID);
        HEAD = Utils.readObject(headPointer, Commit.class);

        //throw error case if no commit with that ID exists
        if (thisCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }

        //Delete (rm) any files that are tracked in CWD but not reset commit
        for (String blob : currentWorkingDir.list()) {
            if (!thisCommit.blobReferences.containsKey(blob) && !blob.equals(".gitlet")) {
                File toRemove = new File(currentWorkingDir.getPath() + "/" + blob);
                Utils.restrictedDelete(toRemove);
            }
        }

        //If there is a file in the CWD that isn't tracked in current branch, throw error:
        //Gives list of all files in branch
        ArrayList<String> branchFiles = new ArrayList<String>();
        Commit currCommit = HEAD;
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        while (currCommit.getParentHash1() != null) {
            for (String blob : currCommit.blobReferences.keySet()) {
                branchFiles.add(blob);
            }
            currCommit = allCommits.get(currCommit.getParentHash1());
        }

        String[] cwdFiles = currentWorkingDir.list();
        for (String file : cwdFiles) {
            if (!branchFiles.contains(file) && !file.equals(".gitlet")) {
                System.out.println("There is an untracked file in the way; " +
                        "delete it, or add and commit it first.");
                return;
            }
        }

        //Check out all blobs at current branch/commit
        for (String blob : thisCommit.blobReferences.keySet()) {
            checkoutID(thisCommit.getCurrID(), blob);
        }


        HEAD = thisCommit;
        HEAD.setName(thisCommit.getName());
        Utils.writeContents(headPointer, Utils.serialize(HEAD));

        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        allBranches.replace(HEAD.getName(), thisCommit);
        Utils.writeContents(allBranchesFile, Utils.serialize(allBranches));

        //clear the staging area
        newBlobReferences = Utils.readObject(newBlobReferencesStorage, TreeMap.class);
        for (String f : newBlobReferences.keySet()) {
            newBlobReferences.remove(f);
        }
        Utils.writeContents(newBlobReferencesStorage, Utils.serialize(newBlobReferences));

    }

    /** Helper method for merge, gets split point/latest common
     * ancestor between current branch and branchName branch.
     * Latest common ancestor rules:
     *  Path exists to commit from both branch heads, and is not
     *  an ancestor of any other common ancestors
     * @param branchName
     * @return
     */
    @SuppressWarnings("unchecked")
    public Commit getSplitPoint(String branchName) {
        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        Commit thisBranchHead = allBranches.get(branchName);
        HEAD = Utils.readObject(headPointer, Commit.class);
        Commit currBranchHead = HEAD;

        Commit splitPoint = null;
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        int distanceFromHead = 0;
        int currIterations = 0;
        //Find all common ancestors of branch heads HEAD and branchName branchhead
        while ((currBranchHead.getParentHash1() != null && thisBranchHead.getParentHash1() != null)
                && !currBranchHead.getCurrID().equals(thisBranchHead.getCurrID())) {

            //if parent #1 of both branches are same
            if (currBranchHead.getParentHash1().equals(thisBranchHead.getParentHash1())) {
                splitPoint = allCommits.get(currBranchHead.getParentHash1());
                distanceFromHead += currIterations;

                currBranchHead = allCommits.get(currBranchHead.getParentHash1());
                thisBranchHead = allCommits.get(thisBranchHead.getParentHash1());

                //if parent #2 of HEAD and parent #1 of other are same
            } else if (currBranchHead.getParentHash2() != null
                    && currBranchHead.getParentHash2().equals(thisBranchHead.getParentHash1())
                    && distanceFromHead <= currIterations) {

                splitPoint = allCommits.get(currBranchHead.getParentHash2());
                distanceFromHead += 1;

                currBranchHead = allCommits.get(currBranchHead.getParentHash2());
                thisBranchHead = allCommits.get(thisBranchHead.getParentHash1());

                //if parent #1 of other and parent #2 of HEAD are same
            } else if (thisBranchHead.getParentHash2() != null
                    && currBranchHead.getParentHash1().equals(thisBranchHead.getParentHash2())
                    && distanceFromHead <= currIterations) {
                splitPoint = allCommits.get(thisBranchHead.getParentHash2());
                distanceFromHead += 1;

                currBranchHead = allCommits.get(currBranchHead.getParentHash1());
                thisBranchHead = allCommits.get(thisBranchHead.getParentHash2());
            } else {
                distanceFromHead += 1;
                currBranchHead = allCommits.get(currBranchHead.getParentHash1());
                thisBranchHead = allCommits.get(thisBranchHead.getParentHash1());
            }
            currIterations += 1;
        }

        if (thisBranchHead == currBranchHead) {
            splitPoint = thisBranchHead;
            return splitPoint;
        }

        Commit initialCommit = null;
        Collection<Commit> commits = allCommits.values();
        for (Commit c : commits) {
            if (c.getCommitMessage().equals("initial commit")) {
                initialCommit = c;
                break;
            }
        }

        if (splitPoint == null && currBranchHead == initialCommit) {
            if (allCommits.get(thisBranchHead.getParentHash1()) == initialCommit) {
                splitPoint = initialCommit;
                return splitPoint;
            }
        } else if (splitPoint == null && thisBranchHead == initialCommit) {
            if (allCommits.get(currBranchHead.getParentHash1()) == initialCommit) {
                splitPoint = initialCommit;
                return splitPoint;
            }
        }

        return splitPoint;
    }

    /** Helper method for merge, to access all files in
     * a given branch since the split point.
     * @param splitPoint
     * @param branch
     * @return
     */
    @SuppressWarnings("unchecked")
    public TreeMap<String, String> getBranchFiles(Commit splitPoint, Commit branch) {
        TreeMap<String, String> allFiles = new TreeMap<String, String>();
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        while (!branch.getCurrID().equals(splitPoint.getCurrID())) {
            for (String fileName : branch.blobReferences.keySet()) {
                allFiles.put(fileName, branch.blobReferences.get(fileName));
            }
            //if branch has two parents, and one is split point, go to split point
            if (branch.getParentHash2() != null
                    && branch.getParentHash1().equals(splitPoint.getCurrID())) {
                branch = splitPoint;
            //if branch has two parents and one is split point, go to split point
            } else if (branch.getParentHash2() != null
                    && branch.getParentHash2().equals(splitPoint.getCurrID())) {
                branch = splitPoint;
            //otherwise, go to branch's parent
            } else {
                branch = allCommits.get(branch.getParentHash1());
            }
        }

        return allFiles;
    }

    /** Helper method for merge, that re-writes the
     * content of files that are in conflict,
     * where fileName is the name of the file in conflict,
     * and branchName is the name of the branch being merged.
     * @param branchName
     * @param fileName
     */
    @SuppressWarnings("unchecked")
    public void writeConflictFile(String branchName, String fileName, Commit splitPoint) throws IOException{
        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);
        Commit toMergeBranch = allBranches.get(branchName);
        File conflictFile = new File(currentWorkingDir+"/"+fileName);

        File copyCurrBranchFile = new File(currentWorkingDir+"/"+fileName+".currBranchCopy");
        BufferedReader currBranchFileReader =
                new BufferedReader(new FileReader(conflictFile));
        String currBranchFileContents = "";
        currBranchFileContents += currBranchFileReader.readLine();
        currBranchFileReader.close();

        Commit temp = toMergeBranch;
        File copyOtherBranchFile = new File(currentWorkingDir+"/"+fileName);
        String otherBranchFileContents = "";
        for (String file : temp.blobReferences.keySet()) {
            if (file.equals(fileName)) {
                Commit tempHead = HEAD;
                checkoutID(temp.getCurrID(), fileName);
                HEAD = tempHead;
                BufferedReader otherBranchFileReader =
                        new BufferedReader(new FileReader(copyOtherBranchFile));
                otherBranchFileContents += otherBranchFileReader.readLine();
                otherBranchFileReader.close();
            }
        }

        Utils.restrictedDelete(copyCurrBranchFile);
        Utils.restrictedDelete(copyOtherBranchFile);

        //clear the contents of conflict file
        File conflictFileFinal = new File(currentWorkingDir+"/"+fileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(conflictFileFinal));
        bw.write("<<<<<<< HEAD");
        bw.write("\n");
        if (!currBranchFileContents.equals("")) {
            bw.write(currBranchFileContents);
        }
        bw.write("\n");
        bw.write("=======");
        bw.write("\n");
        if (!otherBranchFileContents.equals("")) {
            bw.write(otherBranchFileContents);
            bw.write("\n");
        }
        bw.write(">>>>>>>");
        bw.write("\n");
        bw.close();

        }

    /** Helper method for merge-- gets untracked files.
     * A file is untracked if it is not tracked in either
     * branch, and is in the CWD
      */
    @SuppressWarnings("unchecked")
    public boolean getUntrackedFiles(String branchName) {
        String[] cwd = currentWorkingDir.list();
        ArrayList<String> cwdFiles = new ArrayList<String>();
        for (int i = 0; i < cwd.length; i++) {
            if (!cwd[i].equals(".gitlet")) {
                cwdFiles.add(cwd[i]);
            }
        }

        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);

        //return an arraylist of all the hashes of files that have been committed
        Collection<Commit> commits = allCommits.values();
        ArrayList<String> committedFileHashes = new ArrayList<String>();
        for (Commit c : commits) {
            Collection<String> hashes = c.blobReferences.values();
            for (String s : hashes) {
                committedFileHashes.add(s);
            }
        }


        for (String file : cwdFiles) {
             {
                 File tempFile = new File(currentWorkingDir+"/"+file);
                 Blob tempBlob = new Blob(tempFile);
                 String tempBlobHash = tempBlob.getHash();
                 if (!committedFileHashes.contains(tempBlobHash)) {
                     return true;
                 }
             }
        }
        return false;
    }

    /** Merge method-- merge current branch, and branchName
     * @param branchName */
    @SuppressWarnings("unchecked")
    public void merge(String branchName) throws IOException {
        stageForRemoval = Utils.readObject(stageForRemovalStorage, TreeMap.class);
        newBlobReferences = Utils.readObject(newBlobReferencesStorage, TreeMap.class);
        if (!stageForRemoval.keySet().isEmpty() || !newBlobReferences.keySet().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        allBranches = Utils.readObject(allBranchesFile, TreeMap.class);
        if (!allBranches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        Commit currBranch = Utils.readObject(headPointer, Commit.class);
        if (currBranch.getName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        allCommits = Utils.readObject(allCommitsFile, TreeMap.class);

        ArrayList<String> beenTampered = new ArrayList<>();
        if (getUntrackedFiles(branchName)) {
            System.out.println("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");
            return;
        }

        Commit splitPoint = getSplitPoint(branchName);
        Commit otherParent = allCommits.get(allBranches.get(branchName).getParentHash1());
        if (currBranch.getCurrID().equals(otherParent.getCurrID())) {
            checkoutBranch(allBranches.get(branchName).getName());
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (splitPoint == null) {
            Commit temp = currBranch;
            while (temp.getParentHash1() != null) {
                if (temp.getCurrID().equals(allBranches.get(branchName).getCurrID())) {
                    System.out.println("Given branch is an " +
                            "ancestor of the current branch.");
                    return;
                }
                temp = allCommits.get(temp.getParentHash1());
            }
        }
        TreeMap<String, String> currBranchFiles = getBranchFiles(splitPoint, currBranch);
        Commit toMergeBranch = allBranches.get(branchName);
        TreeMap<String, String> toMergeBranchFiles = getBranchFiles(splitPoint, toMergeBranch);

        for (String fileName : splitPoint.blobReferences.keySet()) {
            //if the file is in both branches
            if (currBranchFiles.containsKey(fileName)
                    && toMergeBranchFiles.containsKey(fileName)) {
                //if file has been modified in other, but not HEAD
                if (!toMergeBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName))
                    && currBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName))) {
                    //checkout other's version
                    Commit temp = toMergeBranch;
                    while (!temp.getCurrID().equals(splitPoint.getCurrID())) {
                        if (temp.blobReferences.containsKey(fileName)) {
                            Commit tempHead = HEAD;
                            checkoutID(temp.getCurrID(),fileName);
                            HEAD = tempHead;
                            Utils.writeContents(headPointer, Utils.serialize(HEAD));
                            add(fileName);
                            HEAD = tempHead;
                            break;
                        } else {
                            temp = allCommits.get(toMergeBranch.getParentHash1());
                        }
                    }
                }

                //if file has been modified in HEAD
                else if (toMergeBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName))
                        && !currBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName))) {
                    //do nothing, leave the file as is
                }

                //if file has been modified in both
                else if (!toMergeBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName))
                        && !currBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName))) {
                    //modified in same way (hash values are same)
                    if (toMergeBranchFiles.get(fileName).equals(currBranchFiles.get(fileName))) {
                        //do nothing
                    } else {
                        // modified in different ways, files are in conflict
                        System.out.println("Encountered a merge conflict.");
                        writeConflictFile(branchName, fileName, splitPoint);
                        Commit tempHead = HEAD;
                        add(fileName);
                        HEAD = tempHead;
                    }
                }

            //otherwise, if not currBranch and otherBranch contain the file
            } else {
                //if file is absent from given branch
                if (!toMergeBranchFiles.containsKey(fileName)) {
                    //FILES IN CONFLICT: Contents of one file changed, and other deleted
                    if (currBranchFiles.containsKey(fileName)
                            && !currBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName)))
                    {
                        System.out.println("Encountered a merge conflict.");
                        writeConflictFile(branchName, fileName, splitPoint);
                        Commit tempHead = HEAD;
                        add(fileName);
                        HEAD = tempHead;

                    } else {
                        //stage file for removal (won't be tracked in next commit)
                        //If file exists in staging area, remove it from staging area
                        stageForRemoval = Utils.readObject(stageForRemovalStorage, TreeMap.class);
                        stageForRemoval.put(fileName, "");
                        Utils.writeContents(stageForRemovalStorage, Utils.serialize(stageForRemoval));

                        //remove file from CWD
                        File toRemove = new File(currentWorkingDir.getPath() + "/" + fileName);
                        Utils.restrictedDelete(toRemove);
                    }
                }
                //if file is absent from present branch
                else if (!currBranchFiles.containsKey(fileName)) {
                    //FILES IN CONFLICT: Contents of one file changed, and other deleted
                    if (toMergeBranchFiles.containsKey(fileName)
                            && !toMergeBranchFiles.get(fileName).equals(splitPoint.blobReferences.get(fileName)))
                    {
                        System.out.println("Encountered a merge conflict.");
                        writeConflictFile(branchName, fileName, splitPoint);
                        Commit tempHead = HEAD;
                        add(fileName);
                        HEAD = tempHead;
                    }
                    //do nothing, file should remain absent
                }
            }
            beenTampered.add(fileName);
        }

        //if file is in HEAD, but not splitPoint, or other
        for (String fileName : currBranchFiles.keySet()) {
            if (!splitPoint.blobReferences.containsKey(fileName)
                    && !toMergeBranchFiles.containsKey(fileName)) {
                //do nothing, keep file as is
            } else if (toMergeBranchFiles.containsKey(fileName) &&
                    !currBranchFiles.get(fileName).equals(toMergeBranchFiles.get(fileName))
                    && !beenTampered.contains(fileName)) {
                //FILES IN CONFLICT: File absent in split point, and has different contents in both branches
                System.out.println("Encountered a merge conflict.");
                writeConflictFile(branchName, fileName, splitPoint);
                Commit tempHead = HEAD;
                add(fileName);
                HEAD = tempHead;
            }
            beenTampered.add(fileName);
        }

        //if file is in other, but not splitPoint, or HEAD
        for (String fileName : toMergeBranchFiles.keySet()) {
            if (!splitPoint.blobReferences.containsKey(fileName)
                    && !currBranchFiles.containsKey(fileName)) {
                //checkout file
                Commit temp = toMergeBranch;
                while (!temp.getCurrID().equals(splitPoint.getCurrID())) {
                    if (temp.blobReferences.containsKey(fileName)) {
                        Commit tempHead = HEAD;
                        checkoutID(temp.getCurrID(),fileName);
                        HEAD = tempHead;
                        Utils.writeContents(headPointer, Utils.serialize(HEAD));
                        //stage file for addition
                        add(fileName);
                        HEAD = tempHead;
                        break;
                    } else {
                        temp = allCommits.get(toMergeBranch.getParentHash1());
                    }
                }

            }
            beenTampered.add(fileName);
        }

        //now make a commit. Commit should have two parents:
        //current branch, and merged branch

        String tempID = allBranches.get(branchName).getCurrID();
        String tempName = currBranch.getName();
        Commit tempHead = HEAD;

        TreeMap<String, Commit> tempTree = allBranches;

        commit("Merged " + branchName + " into " + currBranch.getName() + ".");

        HEAD.setName(tempName);
        allBranches = tempTree;
        allBranches.put(HEAD.getName(), HEAD);
        Utils.writeContents(allBranchesFile, Utils.serialize(allBranches));

        HEAD.setParentHash1(tempHead.getCurrID());
        HEAD.setParentHash2(tempID);
        Utils.writeContents(headPointer, Utils.serialize(HEAD));
    }

    /** Accessor method for CWD directory. */
    public File getCurrentWorkingDir() {
        return currentWorkingDir;
    }

    /** Accessor method for allCommits. */
    public TreeMap<String, Commit> getAllCommits() {
        return allCommits;
    }

    /** Accessor method for HEAD. */
    public Commit getHEAD() {
        return HEAD;
    }

}
