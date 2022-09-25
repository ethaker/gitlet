# gitlet
A version control system mimicking the major features of git, implemented in Java.

From January-April 2022, I was having trouble understanding the functionality of Git as a version control system. Implementing its key features on my own, I gained an inside-out understanding of a system that is actually quite simple, elegant and powerful. 

At its core, Gitlet uses hashing to create a chain of commit commit objects referencing one another. Each commit, in turn, uses serialization and hashing to store references to the files created, edited, and deleted at each commit. I implemented the init, add, commit, rm, branch, log, status, checkout, and branch command. Finally, I used a pared-down version of Dijkstra's search algorithm to create an efficient implementation of the merge command.

Please reference the Repo class for the main body of my code.
