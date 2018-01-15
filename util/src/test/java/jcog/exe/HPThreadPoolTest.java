//package jcog.exe;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by I330347 on 8/26/2016.
// */
//public class HPThreadPoolTest {
//
//    public static void main(String[] args){
//        //ConnectionPool Pool = new ConnectionPool(2, 5);
//        BusyPool Pool = new BusyPool(2, qq);
//        Runnable runnable1 = () ->{
//            try {
//                TimeUnit.SECONDS.sleep(10);
//            } catch (InterruptedException e) {
//                //e.printStackTrace();
//                System.out.println("allocataion runable be interruptted");
//                Thread.currentThread().interrupt();
//            }
//            System.out.println("任务"+1);
//        };
//        Runnable runnable2 = () ->{
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                //e.printStackTrace();
//                System.out.println("allocataion runable be interruptted");
//                Thread.currentThread().interrupt();
//            }
//            System.out.println("任务"+2);
//        };
//        Runnable runnable3 = () ->{
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                //e.printStackTrace();
//                System.out.println("allocataion runable be interruptted");
//                Thread.currentThread().interrupt();
//            }
//            System.out.println("任务"+3);
//        };
//        Runnable runnable4 = () ->{
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                //e.printStackTrace();
//                System.out.println("allocataion runable be interruptted");
//                Thread.currentThread().interrupt();
//            }
//            System.out.println("任务"+4);
//        };
//        Runnable runnable5 = () ->{
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                //e.printStackTrace();
//                System.out.println("allocataion runable be interruptted");
//                Thread.currentThread().interrupt();
//            }
//            System.out.println("任务"+5);
//        };
//        Pool.execute(runnable1);
//        Pool.execute(runnable2);
//        Pool.execute(runnable3);
//        Pool.execute(runnable4);
//        Pool.execute(runnable5);
//        /**
//         try {
//         TimeUnit.SECONDS.sleep(5);
//         } catch (InterruptedException e) {
//         e.printStackTrace();
//         }*/
//        Pool.printInfo();
//        Pool.shutdown();
//    }
//
//}