package jcog.lab;

/**
 * Optilive - continuously runnable live optimizer / analyzer
 *   --model, same as Optimize
 *   --parameters, same as Optimize
 *   --goals, same as Optimize
 *
 * initially starts idle with 0 active parameters, 0 active goals
 *
 * when at least 1 parameter and 1 goal are activated, it begins
 * results are collected into a streaming ARFF
 * results are analyzed by zero or more analysis engines, which can include:
 *     a) decision tree
 *     b) nars
 *     c) etc..
 * these derive analyses that are collected in separate logs for each
 *
 *
 * remote control interface thru JMX RPC or something
 */
public class Optilive {

}
