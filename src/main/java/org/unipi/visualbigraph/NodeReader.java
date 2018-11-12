/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author alessandro
 */
public class NodeReader extends Reader{
    private static final Logger LOGGER = Logger.getLogger(NodeReader.class.getName());
    private String regex;
    public NodeReader(ConcurrentLinkedQueue<String> queue, String filePath, char regex){
        super(queue,filePath);
        this.regex = String.valueOf(regex);
    }
    

    @Override
    public void run(){
        try(Scanner scan = new Scanner(new File(this.filePath))) {
            scan.useDelimiter(Pattern.compile(regex));
            while(scan.hasNext()){
                queue.add(scan.next());
            }
        }catch(Exception ex){
            LOGGER.severe(ex.getMessage());
            return;
        }
    }
}
