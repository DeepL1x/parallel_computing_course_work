# parallel_computing_course_work

## Prerequisites
Before running this program, make sure you have Java installed on your machine.

- Download and install Java from [Java's official website](https://www.oracle.com/java/technologies/downloads/).

## Getting Started
1. Download the latest version of program files from the [Releases](https://github.com/DeepL1x/parallel_computing_course_work/releases) section.
2. Save the downloaded files to a location of your choice.

## Running the Program
### Server
#### Command Line
Navigate to the directory where you extracted the program files and run the following command:
```bash
java -jar server.jar <options>
```
#### Avalable options
* ```bash
  -threads <number_of_threads>
  ```
  Specifies amount of threads to fill the inverted index. 4 threads are used by default.
  Replace `number_of_threads` with the actual integer value you want to specify.
* ```bash
  -saveIndex
  ```
  Specifies that you want to save filled index to a file.
  Doesn't work with ```-loadIndex```
* ```bash
  -loadIndex 
  ```
  Specifies that you want to load filled index from a file.
  > Saved index file must be located in savedIndex folder on the same level as jar file and be named index.ser

### Client
#### Command Line
Navigate to the directory where you extracted the program files and run the following command:
```bash
java -jar client.jar
