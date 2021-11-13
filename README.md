# buildNetwork
Building a network diagram with Snowflake UDTFs

Files
-links.csv - sample file to create links or esges
-nodes.csv - sample file to create node objects

SQL
-buildNetwork.java - source code for compiling into JAR with your favorite IDE. OR copy/paste as an inline function in Snowflake
-OUtputRow.java - required object for Java UDTF

SQL
-buildNetwork.sql - UDTF signature for Snowflake
-DataPrep.sql - re-shaping the data as needed by the UDTF and Tableau
