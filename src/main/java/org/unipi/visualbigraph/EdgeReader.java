/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 *
 * @author alessandro
 */
public class EdgeReader extends Reader{     //Qui leggo tutta la linea e in WebGraphUtility la spezzo sul regex;
    private static final Logger LOGGER = Logger.getLogger(EdgeReader.class.getName());
    public EdgeReader(ConcurrentLinkedQueue<String> queue, String filePath){
        super(queue,filePath);
    }
    
    @Override
    public void run(){
        BufferedReader br = null;
        try{
            br = new  BufferedReader(new FileReader(this.filePath));
            String current;
            while( (current = br.readLine()) != null){
                queue.add(current.trim());
            }
        }catch(FileNotFoundException e){
            LOGGER.severe("File not found in: "+filePath);
        }catch(IOException e){
            LOGGER.severe(e.getMessage());
        } finally {
            if(br!=null)
                try {
                    br.close();
            } catch (IOException ex) {
                LOGGER.severe(ex.getMessage());
            }
        }
    }
    
}
