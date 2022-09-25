package gitlet;

import net.sf.saxon.expr.parser.Loc;

import java.io.File;
import java.util.Date;
import java.util.TreeMap;


/**
 * Class that represents a commit object.
 *
 * @author Eesha Thaker
 */
public class Commit implements java.io.Serializable {

    /** Metadata containing commit message **/
    private String commitMessage;

    /** Metadata containing timestamp of commit **/
    private Date timeStamp;

    /** Metadata containing reference to parent #1's
     * commit hash **/
    private String parentHash1;

    /** Metadata containing reference to parent #2's
     * commit hash **/
    private String parentHash2;

    /** A treemap mapping the name of blob files to
     * their "references", or hash values. */
    public TreeMap<String, String> blobReferences;

    /** Represents hash value of this commit.
     * Generated using all metadata: message, timestamp,
     * parent hashes, and treeMap. (using toString() method). */
    public String currID;

    /** Represents the name of a commit object. Only branches have names;
     * branches are pointers to commit objects. If a commit object
     * has a name, it is a branch. */
    private String name;

    /** A new commit object, containing the commit message from that commit,
     * the timestamp, references to HASH VALUES of
     * two possible parents-- if the commit doesn't have two parents
     * then parent #2 will have a value of null. **/
    public Commit(String commitMessage, Date timeStamp, String parent1,
                  String parent2) {
        this.commitMessage = commitMessage;
        this.timeStamp = timeStamp;
        this.parentHash1 = parent1;
        this.parentHash2 = parent2;
        this.blobReferences = new TreeMap<>();
        this.name = null;
        this.currID = null;
    }

    /** Returns commit object's SHA-1 ID */
    public String getCurrID() {
        return this.currID;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public String toString() {
        return commitMessage + timeStamp.toString() + parentHash1 +
                parentHash2;
    }

    /** Sets hash ID for this commit using Utils.sha1 method. */
    public void setCurrID() {
        this.currID = Utils.sha1(this.toString());
    }

    public String getParentHash1() {
        return parentHash1;
    }

    public String getParentHash2() {
        return parentHash2;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    /** Sets timestamp to given argument */
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    /** Sets commit message to given argument */
    public void setCommitMessage(String message) {
        this.commitMessage = message;
    }

    /** Sets timestamp to given argument */
    public void setParentHash1(String parentHash1) {
        this.parentHash1 = parentHash1;
    }

    /** Sets timestamp to given argument */
    public void setParentHash2(String parentHash2) {
        this.parentHash2 = parentHash2;
    }

    /** Sets the name of a commit (declaring that commit object to be a branch) */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the name of a commit. */
    public String getName() {
        return name;
    }

    public static Commit copy(Commit copyFrom) {
        Commit newCommit = new Commit(copyFrom.commitMessage, copyFrom.timeStamp,
                copyFrom.parentHash1, copyFrom.parentHash2);
        newCommit.blobReferences = copyFrom.blobReferences;
        return newCommit;
    }
}
