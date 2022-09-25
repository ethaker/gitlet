package gitlet;

import java.io.File;
import java.io.Serializable;

/** Class representing the blob of a given file.
 * The blob of a file refers to the contents of that file.
 * Class generated to support commit/checkout functionality.
 * Blobs are written to files for persistence.
 * @author Eesha Thaker
 * */

public class Blob implements Serializable {

    /** File that blob represents. */
    private File file;

    /** Hash ID of blob. Generated based on contents of blob,
     * so that blobs generated from the same file (same file name)
     * with different contents generate different hashes.
     * Also used as name of blob. */
    private String blobHash;

    /** Represents path to directory where blob is stored. */
    private String blobPath;

    public Blob(File file) {
        this.file = file;
        this.blobHash = Utils.sha1(Utils.readContentsAsString(file) + file.getName());
        this.blobPath = file.getPath();
    }

    /** Returns hash of given blob. */
    public String getHash() {
        return blobHash;
    }

}
