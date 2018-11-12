/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author alessandro
 */
public abstract class Reader extends Thread{
    protected ConcurrentLinkedQueue<String> queue = null;
    protected String filePath = null;
    public Reader(ConcurrentLinkedQueue<String> queue, String filePath){
        this.queue = queue;
        this.filePath = filePath;
    }
    
    @Override
    public abstract void run();
}
