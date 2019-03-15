package java4k.scramble;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;



public class G extends Applet implements Runnable 
{
    @Override
    public void start()
    {
        new Thread(this).start();
    }

    @Override
    public void run()
    {
        try {
        enableEvents( -1 ); 

        BufferedImage screen = new BufferedImage(224, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D)screen.getGraphics();

        Graphics2D appletG = (Graphics2D)getGraphics();
        appletG.scale( 2, 2 );

        final String[] s_levelData =
        {
            
            "\002\002\002\002\002\002\002\002\002\002\002\002\004\003\003\003" + 
            "\011\001\003\002\011\001\001\001\011\001\001\001\011\001\011\001" + 
            "\011\001\005\005\041\001\005\002\002\002\002\003\003\003\003\003" + 
            "\003\003\003\003\003\003\003\003\001\003\003\003\011\001\011\001" + 
            "\002\005\003\001\031\001\031\001\001\004\011\001\011\001\041\001" + 
            "\002\005\002\003\003\001\001\001\011\001\001\001\011\001\001\001" + 
            "\011\001\041\001\031\001\002\002\003\003\011\001\001\002\002\002" + 
            "\011\001\002\001\011\001\004\005\011\001\004\002\011\001\011\001" + 
            "\002\004\041\001\031\001\002\002\011\001\011\001\004\002\003\002" + 
            "\011\001\011\001\004\002\011\001\011\001\002\002\002\004\003\003" + 
            "\003\003\003\003\003\003\011\001\011\001\002\002\004\004\011\001" + 
            "\002\002\004\005\011\001\002\002\002\002\002\002\004\003\005\003" + 
            "\011\001\003\003\031\001\003\003\003\005\011\001\003\002\002\002" + 
            "\011\001\041\001\002\003\011\001\004\003\003\003\003\003\003\003" + 
            "\001\003\011\001\005\004\031\001\031\001\002\002\011\001\001\002" + 
            "\002\003\001\002\002\003\003\003\003\001\011\001\001\005\001\004" + 
            "\003\003\003\002\031\001\003\001\041\001\004\002\011\001\002\002" + 
            "\011\001\041\001\003\005\002\001\002\002\002\002\004\005\011\001" + 
            "\011\001\041\001\031\001\005\003\011\001\011\001\011\001\001\001" + 
            "\004\002\031\001\011\001\001\001\003\003\003\005\031\001\002\002" + 
            "\002\004\002\002\002\005\002\004\003\003\003\003\003\003\003\003",

            
            "",

            
            "\003\001\001\003\031\001\003\001\001\002\002\002\002\002\001\005" + 
            "\004\003\005\004\003\003\001\003\003\002\002\002\002\003\003\002" + 
            "\003\003\002\003\002\004\002\002\002\002\003\002\001\003\003\002" + 
            "\003\003\001\002\003\005\002\003\002\002\002\001\003\003\003\003" + 
            "\011\001\031\001\011\001\031\001\001\003\041\001\041\001\002\001" + 
            "\011\001\002\002\011\001\002\002\001\001\001\001\003\003\001\003" + 
            "\041\001\011\001\003\001\041\001\001\003\011\001\031\001\002\002" + 
            "\002\002\002\001\003\003\003\003\011\001\011\001\031\001\031\001" + 
            "\001\003\011\001\011\001\002\001\041\001\002\002\011\001\002\002" + 
            "\001\001\001\001\003\003\001\003\041\001\041\001\003\001\011\001" + 
            "\001\003\041\001\041\001\002\002\003\003\003\001\011\001\001\004" + 
            "\031\001\011\001\001\001\004\001\041\001\011\001\041\001\002\002" + 
            "\002\002\002\004\003\003\003\003\003\004\011\001\031\001\002\004" + 
            "\003\001\011\001\011\001\011\001\001\004\041\001\001\002\002\002",

            
            "\003\001\001\003\001\001\003\001\001\003\003\002\003\003\001\003" + 
            "\001\002\002\002\003\003\002\003\003\002\003\002\002\002\003\002" + 
            "\002\002\003\003\003\002\002\002\003\003\002\002\002\003\003\002" + 
            "\002\003\003\001\002\002\001\002\003\003\003\003\003\003\003\001" + 
            "\001\002\001\001\003\001\001\001\001\001\001\002\002\002\002\002" + 
            "\001\001\001\001\001\001\001\002\001\001\001\001\003\001\001\001" + 
            "\003\003\003\001\001\003\003\001\001\002\002\002\002\002\002\002" + 
            "\003\003\003\003\003\003\003\001\001\002\001\001\003\001\001\001" + 
            "\001\001\001\002\002\002\002\002\001\001\001\001\001\001\001\002" + 
            "\001\001\001\001\003\001\001\001\003\003\003\001\001\003\003\001" + 
            "\001\002\002\002\002\002\002\002\003\003\003\003\003\003\001\002" + 
            "\002\002\002\002\001\003\001\002\001\001\001\003\003\003\003\001" + 
            "\002\002\002\002\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\002",

            
            "\001\003\003\003\011\001\002\002\005\002\002\002\003\003\003\003" + 
            "\003\001\011\001\031\001\011\001\002\002\003\003\002\002\004\003" + 
            "\031\001\002\002\002\002\003\002\003\003\003\003\011\001\001\003" + 
            "\041\001\031\001\001\002\002\002\002\004\003\003\003\003\031\001" + 
            "\002\005\003\001\041\001\041\001\001\004\031\001\011\001\011\001" + 
            "\002\005\002\003\003\001\041\001\041\001\001\001\031\001\001\001" + 
            "\002\002\002\002\002\003\003\003\003\003\031\001\001\002\002\002" + 
            "\001\003\003\003\041\001\002\002\005\002\002\002\003\003\003\003" + 
            "\003\001\031\001\011\001\011\001\002\002\003\003\002\002\004\003" + 
            "\031\001\002\002\002\002\003\002\003\003\003\003\001\001\001\003" + 
            "\041\001\031\001\001\002\002\002\001\003\003\003\011\001\002\002" + 
            "\005\002\002\002\003\003\003\003\003\001\011\001\011\001\031\001" + 
            "\002\002\003\003\002\002\004\003\031\001\002\002\002\002\003\002" + 
            "\003\003\003\003\001\001\001\003\031\001\041\001\001\002\002\002",

            
            "",

            
            "\001\211\001\001\001\001\001\001\001\171\011\211\011\001\001\161" + 
            "\011\211\011\241\001\221\011\241\001\221\011\001\001\201\011\001" + 
            "\031\161\011\001\011\141\001\001\031\121\011\001\001\131\001\001" + 
            "\041\001\001\001\001\111\011\151\001\131\011\171\001\151\011\211" + 
            "\001\171\011\231\001\211\011\231\001\241\001\221\011\231\001\001" + 
            "\001\211\011\221\001\201\011\211\001\171\011\201\001\161\011\171" + 
            "\001\151\011\001\001\001\001\001\041\001\031\001\041\001\011\001" + 
            "\011\161\011\171\031\201\001\211\011\001\001\221\001\001\001\211" + 
            "\011\241\001\001\001\231\011\241\011\221\011\241\001\231\011\241" + 
            "\001\211\011\231\031\251\001\211\011\251\001\211\011\251\001\231" + 
            "\011\251\001\231\011\211\001\001\031\001\001\001\031\001\001\001" + 
            "\031\171\011\211\011\001\001\161\011\211\011\241\001\221\011\241" + 
            "\001\221\011\001\001\201\011\001\001\161\011\001\011\141\011\001" + 
            "\031\121\011\001\001\131\001\001\041\001\011\001\001\111\011\151" + 
            "\001\131\011\171\001\151\011\211\001\171\011\231\001\211\011\231" + 
            "\001\241\001\221\011\231\001\001\001\211\011\221\001\201\011\211" + 
            "\001\171\011\201\001\161\011\171\001\151\011\001\001\001\001\001" + 
            "\041\001\031\001\041\001\011\001\011\161\011\171\001\201\041\211" + 
            "\001\001\011\221\001\001\001\211\001\241\001\001\001\231\011\241" + 
            "\011\221\011\241\001\231\011\241\001\211\011\231\031\251\001\211" + 
            "\011\251\001\211\011\251\001\231\011\211\001\001\001\001\001\001" + 
            "\001\001",

            
            "",

            
            "\001\211\031\001\031\001\001\001\001\001\001\231\031\001\001\211" + 
            "\001\001\001\001\001\001\001\001\001\001\031\001\031\001\031\001" + 
            "\031\001\001\001\001\251\001\001\001\001\001\001\001\001\001\111" + 
            "\001\001\001\001\001\001\001\001\031\001\031\001\031\001\031\001" + 
            "\031\001\031\001\001\001\001\001\001\021\001\001\001\001\001\001" + 
            "\001\001\031\001\031\001\031\001\031\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\231\001\001\001\001\001\001\001\001" + 
            "\001\021\001\001\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\211\001\001\001\001\001\001\001\001" + 
            "\001\241\001\001\001\001\001\221\001\001\001\001\001\001\001\001" + 
            "\001\001\001\021\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\231\001\211\001\001\001\001" + 
            "\001\041",

            
            "\001\251\001\001\001\271\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\251\001\001\001\001\001\001\001\001\001\301" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\271\001\141\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\051" + 
            "\001\001\001\001\001\001\001\001\001\001\001\261\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\051" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\241\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\271\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\251\001\001\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\051\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\261\001\001" + 
            "\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001\001" + 
            "\001\311",

            
            "\001\001\001\131\001\231\001\131\001\041\001\001\001\121\001\001" + 
            "\001\001\001\041\001\241\001\001\001\001\001\041\001\121\001\001" + 
            "\001\001\001\041\001\001\001\001\051\001\001\001\001\001\001\171" + 
            "\001\001\001\041\001\001\001\001",

            
            "",
        };

        for( int i = 0; i < s_levelData.length; i += 2 )
        {
            while( s_levelData[i + 1].length() < s_levelData[i].length() )
                s_levelData[i + 1] += "\01";
        }

        int[] colorTable = new int[6];
        
        colorTable[1] = 0xffffffff;
        colorTable[2] = 0xffff0000;
        colorTable[3] = 0xff00def7;
        colorTable[4] = 0xffff5500;
        colorTable[5] = 0xffff00ff;
        String graphicData =
            "\01\01\01\02\06\01\01\01\01\01\03\05\01\01\01" +
            "\01\01\01\03\01\01\01\01\01\01\01\05\01\01\01" +
            "\01\01\01\01\02\01\03\01\04\01\01\01\01\01\01" +
            "\01\01\01\01\04\01\01\04\01\01\01\02\03\05\06" +
            "\01\01\01\03\02\06\05\01\01\01\03\01\01\01\01" +
            "\01\01\01\01\01\01\01\05\01\01\01\02\01\03\01" +
            "\01\01\01\01\04\01\01\01\01\01\01\01\01\04\06" +
            "\02\04\01\01\01\04\05\01\06\01\01\01\04\06\01" +
            "\05\01\01\03\06\01\01\03\06\02\05\01\01\03\06" +
            "\01\01\05\01\01\02\02\01\02\06\01\02\06\05\01" +
            "\01\01\01\01\01\01\04\01\06\02\04\01\01\01\04" +
            "\05\01\06\01\01\01\04\06\01\05\01\03\01\06\02" +
            "\01\02\06\02\05\02\01\02\06\01\01\01\05\02\01" +
            "\02\01\02\06\01\02\06\02\04\01\01\01\01\01\01" +
            "\04\06\01\02\04\01\01\01\04\05\01\06\01\01\01" +
            "\04\06\01\05\01\03\01\06\01\02\02\06\02\05\01" +
            "\02\02\06\01\01\01\06\01\01\01\01\01\03\01\01" +
            "\01\01\01\04\01\01\01\01\04\01\06\01\02\04\01" +
            "\01\01\02\01\01\06\01\01\01\03\01\01\05\01\03" +
            "\01\06\02\01\02\06\02\05\02\01\02\06\01\01\01" +
            "\05\01\01\03\06\03\06\02\01\04\01\01\01\01\01" +
            "\01\01\04\06\01\01\02\01\04\01\04\05\01\01\03" +
            "\04\01\04\06\01\01\02\04\01\03\06\02\01\02\01" +
            "\01\05\01\01\03\01\01\06\05\01\01\01\01\01\02" +
            "\03\04\01\01\01\01\01\01\01\01\04\01\06\01\01" +
            "\01\02\01\01\01\05\01\01\03\01\01\01\06\01\01" +
            "\02\01\04\01\03\01\01\01\01\01\01\01\01\01\01" +
            "\01\05\01\01\01\01\01\01\01\01\01\01\01\01\01" +
            "\01\01\01\01\04\06\01\01\01\01\01\02\01\01\05" +
            "\01\01\03\01\01\01\06\01\01\02\01\04\01\01\01" +
            "\03\01\01\01\01\01\01\01\05\01\01\01\04\01\06" +
            "\01\01\01\01\01\01\01\01\01\01\01\02\01\01\01" +
            "\04\01\01\01\01\04\01\01\05\01\01\03\01\01\01" +
            "\06\01\01\02\01\04\01\01\01\01\02\01\01\01\01" +
            "\01\06\01\01\01\01\01\04\01\01\06\01\01\01\01" +
            "\01\01\01\02\01\01\04\03\02\04\01\01\01\01\04" +
            "\01\01\04\01\01\04\01\01\01\05\06\02\03\01\04" +
            "\01\01\01\02\01\06\01\01\01\02\01\06\01\01\01" +
            "\01\01\01\04\01\06\01\01\01\01\01\02\01\04\01" +
            "\01\03\02\04\01\01\01\01\04\01\01\04\01\01\04" +
            "\01\01\01\04\02\01\03\01\04\01\01\01\02\01\01" +
            "\01\01\01\01\01\06\01\01\01\01\01\01\01\04\06" +
            "\01\01\01\01\01\02\04\01\01\01\03\02\01\04\01" +
            "\01\04\01\01\04\01\01\01\01\04\01\04\02\06\01" +
            "\01\04\04\01\01\02\01\06\01\01\01\01\01\02\01" +
            "\06\01\01\01\01\01\01\04\06\01\01\01\01\01\02" +
            "\04\01\01\01\03\01\02\04\01\04\01\06\02\04\01" +
            "\01\01\01\04\01\04\01\02\01\06\04\04\01\01\02" +
            "\01\01\01\01\01\01\01\01\01\06\01\01\01\01\01" +
            "\01\04\01\06\01\01\01\02\01\04\01\01\01\03\01" +
            "\02\01\04\04\06\01\02\04\01\01\01\01\04\01\04" +
            "\02\06\02\06\04\04\01\02\01\06\01\01\01\01\01" +
            "\01\01\02\01\06\01\01\01\01\01\01\04\01\06\01" +
            "\02\01\04\01\01\01\01\03\01\01\02\04\04\06\01" +
            "\02\04\01\01\01\01\04\01\04\01\02\06\01\04\04" +
            "\02\01\01\01\06\01\01\01\01\01\02\01\01\01\06" +
            "\01\01\01\01\01\01\04\01\01\01\04\01\01\01\01" +
            "\01\03\01\01\02\01\01\06\01\05\01\01\01\01\01" +
            "\01\04\05\06\01\01\01\01\01\01\01\01\01\01\01" +
            "\04\01\01\06\01\01\05\01\01\01\01\01\01\01\01" +
            "\01\01\01\01\01\01\01\01\01\01\01\01\01\01\01" +
            "\01\04\01\04\01\01\01\01\01\04\05\01\01\06\01" +
            "\01\01\01\01\01\01\01\04\01\01\01\06\01\01\01" +
            "\01\01\05\01\01\01\01\01\01\01\01\01\01\01\01" +
            "\01\01\01\01\01\01\01\01\01\04\01\06\05\01\01" +
            "\01\04\01\01\01\05\01\01\01\06\01\01\01\01\01" +
            "\04\05\03\05\01\02\01\06\01\02\06\02\05\01\01" +
            "\01\01\03\05\01\01\01\01\03\05\01\01\01\01\01" +
            "\01\01\04\01\06\01\05\01\01\01\01\04\06\06\01" +
            "\02\06\06\01\01\01\01\01\04\01\05\03\05\01\02" +
            "\01\06\01\02\06\02\01\05\01\01\01\01\03\05\02" +
            "\06\01\01\01\01\01\01\01\01\01\01\04\06\01\01" +
            "\05\01\01\01\01\04\01\05\01\01\01\06\01\01\01" +
            "\01\04\01\01\01\01\01\01\01\01\01\06\01\01\01" +
            "\01\01\01\05\01\01\01\03\05\01\01\01\02\06\01" +
            "\03\05\01\01\01\04\06\01\01\05\01\04\04\04\01" +
            "\05\03\06\06\01\01\06\02\06\01\01\01\04\01\01" +
            "\05\01\02\01\06\01\02\01\05\01\01\01\01\01\01" +
            "\01\02\01\01\06\02\06\01\03\05\01\01\01\04\01" +
            "\06\01\01\05\04\04\04\01\05\03\01\06\01\06\01" +
            "\01\06\02\06\03\01\05\01\01\04\01\01\06\01\01" +
            "\05\01\01\03\01\05\01\01\01\01\02\02\06\01\01" +
            "\02\01\05\01\01\04\01\01\06\01\01\01\02\05\02" +
            "\06\01\03\01\06\01\01\01\06\01\02\06\01\06\03" +
            "\01\01\05\01\01\01\01\01\01\01\03\01\01\05\01" +
            "\03\05\02\01\02\03\04\02\06\06\01\01\01\04\01" +
            "\06\01\01\01\01\01\02\05\02\06\01\03\01\06\01" +
            "\01\01\06\01\02\06\01\06\04\01\05\01\01\03\06" +
            "\01\01\06\01\01\02\01\05\01\01\01\01\02\04\04" +
            "\02\03\05\06\06\01\01\04\01\04\01\01\01\01\01" +
            "\01\04\04\04\05\01\03\01\06\01\06\01\01\06\02" +
            "\06\01\04\01\05\01\01\03\06\01\01\06\01\01\02" +
            "\01\05\01\01\01\02\02\03\04\01\04\05\06\06\01" +
            "\01\03\02\01\04\01\01\01\01\01\01\04\04\04\05" +
            "\01\03\06\06\01\01\06\02\06\01\01\04\04\01\03" +
            "\01\01\02\06\02\01\01\04\01\03\05\01\01\01\03" +
            "\06\02\03\01\04\01\06\01\01\01\03\01\02\01\04" +
            "\01\01\01\01\01\01\01\03\02\01\05\01\01\01\02" +
            "\05\01\01\01\01\04\04\01\01\03\01\02\06\02\01" +
            "\04\01\01\03\05\01\01\01\03\06\01\01\02\01\06" +
            "\06\01\01\01\03\01\01\02\04\01\01\01\01\04\05" +
            "\02\05\01\01\04\05\06\01\01\03\06\01\06\01\04" +
            "\04\01\01\01\01\04\06\05\01\01\01\01\03\05\01" +
            "\01\03\05\01\01\02\01\06\02\02\05\01\01\03\01" +
            "\01\02\04\01\01\01\04\05\01\01\06\01\04\05\01" +
            "\01\06\01\04\05\01\01\03\05\01\06\01\01\04\05" +
            "\01\06\01\01\01\04\05\01\06\03\05\01\01\03\05" +
            "\01\01\01\03\05\01\01\03\01\01\02\01\04\01\01" +
            "\04\05\01\01\06\01\04\05\01\01\06\01\04\05\01" +
            "\01\03\05\01\06\01\01\04\05\01\06\01\01\01\04" +
            "\05\01\06\01\01\01\01\03\05\01\01\01\03\05\01" +
            "\01\03\01\01\01\02\01\01\04\01\04\05\06\01\01" +
            "\01\04\05\06\01\01\01\04\05\06\04\05\01\06\01" +
            "\01\04\05\01\06\01\01\01\04\05\01\02\05\01\01" +
            "\01\01\01\01\03\05\01\03\05\01\03\01\01\01\01" +
            "\01\02\01\04\01\01\01\01\05\03\01\01\05\02\03" +
            "\06\06\01\01\04\01\01\05\01\01\01\01\01\01\01" +
            "\01\01\01\01\01\03\05\01\01\01\01\01\01\01\01" +
            "\01\01\03\05\04\01\04\04\04\04\01\01\04\01\02" +
            "\04\01\02\06\01\01\03\02\04\01\03\02\01\06\01" +
            "\01\01\01\01\01\01\01\01\01\01\01\01\01\01\01" +
            "\01\01\01\01\01\01\05\03\01\01\01\01\01\03\02" +
            "\01\01\01\01\06\01\03\01\03\06\06\01\04\04\02" +
            "\03\04\01\04\06\01\02\01\06\01\01\01\04\01\05" +
            "\01\01\06\01\01\01\01\01\01\01\01\01\05\01\01" +
            "\03\01\01\01\01\01\01\03\01\01\01\01\01\01\01" +
            "\05\01\01\01\01\01\05\01\03\05\02\06\01\03\01" +
            "\05\04\01\06\03\01\02\06\01\06\02\01\06\06\01" +
            "\01\01\01\01\02\05\03\04\03\05\01\01\03\01\01" +
            "\06\04\01\01\01\01\01\01\01\05\01\01\01\01\06" +
            "\02\01\01\01\01\02\06\01\02\01\01\01\01\01\02" +
            "\02\06\01\06\02\01\06\01\01\01\06\01\06\06\04" +
            "\04\02\06\03\05\03\05\02\02\01\03\01\01\01\01" +
            "\01\01\01\02\05\03\04\04\04\01\02\06\02\03\04" +
            "\01\01\06\01\04\01\01\06\01\01\01\01\01\01\01" +
            "\01\01\01\01\01\04\04\01\05\03\04\04\04\02\01" +
            "\06\01\02\02\03\01\01\01\01\01\01\01\05\02\06" +
            "\04\05\01\06\02\01\03\01\01\01\04\02\01\01\01" +
            "\01\01\03\01\05\01\01\01\01\01\01\01\01\06\06" +
            "\06\04\04\01\04\05\06\01\01\04\04\03\05\05\01" +
            "\01\01\01\01\01\01\06\03\05\06\02\03\01\01\04" +
            "\02\03\04\01\01\01\05\04\01\06\04\06\01\02\06" +
            "\01\06\02\06\06\01\01\01\02\04\02\03\05\06\04" +
            "\05\01\06\01\02\02\01\03\01\01\01\01\01\01\01" +
            "\02\01\01\04\01\04\01\04\01\01\01\01\01\01\01" +
            "\02\01\06\01\01\01\04\01\05\01\01\06\01\01\01" +
            "\01\01\01\02\04\01\04\04\01\03\05\03\01\01\01" +
            "\06\04\02\01\01\01\01\06\01\05\04\04\01\01\01" +
            "\01\01\01\01\01\01\01\01\02\01\06\01\01\01\01" +
            "\01\01\01\01\01\01\01\01\01\01\01\01\01\02\05" +
            "\06\04\04\03\01\06\02\01\01\01\04\01\04\04\04" +
            "\04\01\01\04\04\04\01\01\01\01\01\01\01\01\01" +
            "\01\06\01\01\04\01\01\05\01\01\01\01\01\01\01" +
            "\01\01\01\01\01\01\01\01\01\01\05\01\01\03\01" +
            "\01\01\01\01\01\01\01\01\01\01\01\01";

        BufferedImage data = new BufferedImage(54, 43, BufferedImage.TYPE_INT_ARGB);
        {
            int c = 0;
            for( int idx = 0; idx < 43 * 54; idx++ )
            {
                int delta = graphicData.charAt(idx) - 1;
                c = (c + delta) % 6;
                data.setRGB( idx % 54, idx / 54, colorTable[c] );
            }
        }

        final int SHIP      = 1;
        final int SHOT      = 2;
        final int BOMB_H    = 3;
        final int BOMB_D    = 4;
        final int BOMB_V    = 5;
        final int SHIP_FLAME = 6;
        final int EXPLODE   = 7;
        final int ROCKET    = 8;
        final int ROCKET_FLYING = 9;
        final int FUEL      = 10;
        final int MYSTERY   = 11;
        final int BASE      = 12;
        final int SAUCER    = 13;
        final int METEOR    = 14;
        final int G_FLAT    = 15;
        final int G_UP      = 16;
        final int G_DOWN    = 17;
        final int G_PEAK    = 18;
        final int G_HOLE    = 19;
        final int GT_FLAT   = 20;
        final int GT_UP     = 21;
        final int GT_DOWN   = 22;

        final int N_IMG = 23;
        BufferedImage[] sii = new BufferedImage[N_IMG];
        int[] siox = new int[N_IMG];
        int[] sioy = new int[N_IMG];
        int[] sibl = new int[N_IMG];
        int[] sibt = new int[N_IMG];
        int[] sibr = new int[N_IMG];
        int[] sibb = new int[N_IMG];

        String siData =
            "\15\40\23\13\10" + 

            "\04\04\01\01\04" + 

            "\07\45\06\03\06" + 
            "\03\45\04\04\06" + 
            "\00\45\03\06\06" + 

            "\00\40\15\05\10" + 
            "\40\22\16\17\10" + 

            "\00\00\07\20\10" + 
            "\07\00\07\20\10" + 

            "\16\00\20\20\10" + 
            "\00\20\20\20\10" + 
            "\20\20\20\20\10" + 

            "\36\00\14\07\10" + 
            "\40\42\16\11\10" + 

            "\56\40\10\10\10" + 
            "\56\20\10\10\10" + 
            "\56\30\10\10\10" + 
            "\56\00\10\10\10" + 
            "\56\10\10\10\10" + 

            "\56\42\10\10\10" + 
            "\46\10\10\10\10" + 
            "\36\10\10\10\10";  

        int idx = 0;
        for( int i = 1; i < N_IMG; i++ )
        {
            int x = siData.charAt(idx++);
            int y = siData.charAt(idx++);
            int w = siData.charAt(idx++);
            int h = siData.charAt(idx++);
            int b = siData.charAt(idx++) - 6;
            

            siox[i] = -w / 2;
            sioy[i] = -h / 2;
            sii[i] = data.getSubimage(x, y, w, h);
            sibl[i] = siox[i] + b;
            sibt[i] = sioy[i] + b;
            sibr[i] = siox[i] + w - b;
            sibb[i] = sioy[i] + h - b;
            if( i >= G_FLAT )
                if( i < GT_FLAT )
                    sibb[i] += 256;
                else
                    sibt[i] -= 256;
        }

        final int MAX_S = 100;
        int[] st = new int[MAX_S];
        int[] sx = new int[MAX_S];
        int[] sy = new int[MAX_S];
        int[] sd = new int[MAX_S];

        int x = 0;

        int shipx = 0;
        int shipy = 0;

        boolean shot = false;
        boolean bomb = false;

        final int MODE_GAMEOVER     = 0;
        final int MODE_NEWGAME      = 1;
        final int MODE_RETRYLEVEL   = 2;
        final int MODE_PLAYING      = 3;

        final int VIEW_TOP = 30;
        final int VIEW_BOT = VIEW_TOP + 25 * 8;
        final int VIEW_MID = (VIEW_TOP + VIEW_BOT) / 2;

        int     level = 0;
        int     score = 0;
        int     hiscore = 10000;
        int     fuel  = 0;
        int     ships = 0;
        int     mode = MODE_GAMEOVER;
        String  levelBotData = null;
        String  levelTopData = null;
        int     sceneryBH = 0;
        int     sceneryTH = 0;
        int     bombCount = 0;
        boolean hitBase = false;


        long    start   = System.nanoTime();
        int     count = 0;
        for(;;)
        {
            int dw = getWidth();
            int dh = getHeight();
    
            g2.setColor( new Color(0f, 0f, 0f) );
            g2.fillRect( 0, 0, dw, dh );

            g2.setColor( new Color(1f, 1f, 1f) );

            if( mode == MODE_GAMEOVER && m_keysDown['1'] )
            {
                mode = MODE_NEWGAME;
            }

            if( mode == MODE_NEWGAME )
            {
                level = 0;
                score = 0;
                ships = 3;

                mode = MODE_RETRYLEVEL;
            }

            if( mode == MODE_RETRYLEVEL )
            {
                mode = MODE_PLAYING;
                fuel = 2500;
                levelBotData = s_levelData[level * 2];
                levelTopData = s_levelData[level * 2 + 1];
                x = 0;
                st[0] = SHIP;
                shipx = 50;
                shipy = VIEW_TOP + 48;
                hitBase = false;
                for( int i = 1; i < MAX_S; i++ )
                    st[i] = 0;
                for( int i = 0; i < 29; i++ )
                    addSprite( st, sx, sy, sd, G_FLAT, i * 8 - 4, VIEW_BOT - 5 * 8 + 4 );
                sceneryBH = 5;
                sceneryTH = 26;
            }

            if( (m_keysDown[KeyEvent.VK_LEFT] || m_keysDown['A']) && shipx > 20 )
                shipx--;
            if( (m_keysDown[KeyEvent.VK_RIGHT] || m_keysDown['D']) && shipx < 108 )
                shipx++;
            if( (m_keysDown[KeyEvent.VK_UP] || m_keysDown['W']) && shipy > VIEW_TOP + 16 )
                shipy--;
            if( (m_keysDown[KeyEvent.VK_DOWN] || m_keysDown['S']) && shipy < VIEW_BOT )
                shipy++;

            if( st[0] == SHIP )
            {
                boolean newShot = m_keysDown[KeyEvent.VK_SHIFT] || m_keysDown[' '];
                if( !shot && newShot )
                {
                    addSprite( st, sx, sy, sd, SHOT, x + shipx + 9, shipy );
                }
                shot = newShot;
    
                boolean newBomb = m_keysDown[KeyEvent.VK_CONTROL] ||
                                  m_keysDown['\\'] || m_keysDown['/'];
                if( !bomb && newBomb )
                {
/*                  int bombCount = 0;
                    for( int i = 0; i < st.length; i++ )
                        if( st[i] == BOMB_H || st[i] == BOMB_D || st[i] == BOMB_V )
                            bombCount++;
*/
                    if( bombCount < 2 )
                        addSprite( st, sx, sy, sd, BOMB_H, x + shipx - 3, shipy + 4 );
                }
                bomb = newBomb;

                if( (x & 7) == 0 )
                {
                    int c = x / 8;
                    if( c >= levelBotData.length() )
                    {
                        
                        if( level < 5 )
                            level++;
                        levelBotData += s_levelData[level * 2];
                        levelTopData += s_levelData[level * 2 + 1];
                    }

                    char bch = levelBotData.charAt(c);
                    char tch = levelTopData.charAt(c);
    
                    int sceneryB = (bch & 7) - 1 + G_FLAT;
                    int sceneryT = (tch & 7) - 1 + GT_FLAT;

                    

                    int X = x + 28 * 8;
                    int YB = VIEW_BOT - sceneryBH * 8;
                    int YT = VIEW_BOT - sceneryTH * 8;
    
                    int spriteB = (bch >> 3);
                    if( spriteB != 0 )
                    {
                        if( (c & 1) == 0 )
                            addSprite( st, sx, sy, sd, spriteB + EXPLODE, X + 8, YB - 8 );
                        else
                            sceneryBH = spriteB + 1;
                    }

                    int spriteT = (tch >> 3);
                    if( spriteT != 0 )
                    {
                            sceneryTH = spriteT + 1;
                    }

                    if( sceneryB == G_UP || sceneryB == G_PEAK )
                        addSprite( st, sx, sy, sd, sceneryB, X + 4, YB - 4 );
                    else
                        addSprite( st, sx, sy, sd, sceneryB, X + 4, YB + 4 );

                    if( sceneryT == GT_DOWN )
                        addSprite( st, sx, sy, sd, GT_DOWN, X + 4, YT + 12 );
                    else
                        addSprite( st, sx, sy, sd, sceneryT, X + 4, YT + 4 );

                    if( sceneryB == G_UP )
                        sceneryBH++;
                    else if( sceneryB == G_DOWN )
                        sceneryBH--;
                    if( sceneryT == GT_UP )
                        sceneryTH++;
                    else if( sceneryT == GT_DOWN )
                        sceneryTH--;
                }
    
                if( level == 1 && x % 50 == 0 )
                {
                    addSprite( st, sx, sy, sd, SAUCER, x + 224, VIEW_MID );
                }
    
                if( level == 2 && x % 15 == 0 )
                {
                    addSprite( st, sx, sy, sd, METEOR, x + 224, (int)(VIEW_TOP + Math.random() * 135) );

                }

                x++;
                if( fuel > 0 )
                    fuel--;
                else
                    shipy++;
            }
    


            



            g2.setColor( new Color(1f, 1f, 0f) );
            g2.drawString( "SCORE " + score, 30, 10 );
            g2.drawString( "HI " + hiscore, 150, 10 );

            for( int i = 0; i < 6; i++ )
            {
                g2.setColor( new Color(1f, 0f, 0f) );
                if( i > level )
                    g2.setColor( new Color(1f, 0f, 1f) );
                g2.fillRect( i * 30 + 20, VIEW_TOP - 9, 30, 8 );
                g2.setColor( new Color(1f, 1f, 0f) );
                g2.drawRect( i * 30 + 20, VIEW_TOP - 9, 30, 8 );
            }

/*
            g2.setColor( new Color(1f, 0f, 0f) );
            
            {
                int c = x / 8 + i;
                char ch = level1Data.charAt(c);
                int X = c * 8 - x;
                int Y = 256 - levelH[c] * 8;

                int sprite = (ch >> 3);
                if( sprite != 0 && i == 28 && (x & 7) == 0 )
                    addSprite( st, sx, sy, sd, sprite + EXPLODE, x + i * 8 + 8, Y - 8 );
                
                int scenery = (ch & 7) - 1 + G_PEAK;
                if( scenery == G_UP || scenery == G_PEAK )
                {
                    g2.drawImage( sii[scenery], X, Y - 8, null );
                    g2.fillRect( X, Y, 8, 256 - Y );
                }
                else
                {
                    g2.drawImage( sii[scenery], X, Y, null );
                    g2.fillRect( X, Y + 8, 8, 256 - Y );
                }
            }
*/
            g2.setColor( new Color(1f, 0f, 0f) );

            bombCount = 0;
            for( int i = st.length - 1; i >= 0; i-- )
            {
                int t = st[i];
                
                if( t != 0 )
                {
                    for( int j = 0; j < st.length; j++ )
                    {
                        int tj = st[j];
                        if( i == j || tj == 0 || t >= EXPLODE || tj <= EXPLODE )
                            continue;

                        
                        if( sx[i] + sibl[t]  > sx[j] + sibr[tj] ||
                            sx[j] + sibl[tj] > sx[i] + sibr[t]  ||
                            sy[i] + sibt[t]  > sy[j] + sibb[tj] ||
                            sy[j] + sibt[tj] > sy[i] + sibb[t] )
                            continue;

                        
                        st[i] = t == SHOT ? 0 : EXPLODE;
                        sd[i] = 0;

                        if( tj < METEOR )
                        {
                            st[j] = EXPLODE;
                            sd[j] = 0;
                        }
                        int oldScore = score;
                        if( tj == ROCKET )
                            score += 50;
                        if( tj == ROCKET_FLYING )
                            score += 80;
                        if( tj == SAUCER )
                            score += 100;
                        if( tj == FUEL )
                        {
                            fuel = fuel < 2000 ? fuel + 500 : 2500;
                            score += 150;
                        }
                        if( tj == MYSTERY )
                            score += (int)(Math.random() * 3 + 1) * 100;

                        if( oldScore < 10000 && score >= 10000 )
                            ships++;
                        if( hiscore < score )
                            hiscore = score;

                        if( tj == BASE )
                        {
                            hitBase = true;
                            score += 800;
                        }
                    }

                    if( t == ROCKET && (level == 0 || level == 3) &&
                        sx[i] - x < 120 && Math.random() < 0.02f )
                    {
                        st[i] = t = ROCKET_FLYING;
                    }

                    if( t == ROCKET_FLYING )
                    {
                        sy[i]--;
                    }

                    if( t == SHOT )
                    {
                        sx[i] += 4;
                    }

                    if( t == BOMB_V )
                    {
                        bombCount++;
                        sx[i] += 1;
                        sy[i] += 1;
                    }

                    if( t == BOMB_D )
                    {
                        bombCount++;
                        sx[i] += 2;
                        sy[i] += 1;
                        if( sd[i]++ > 28 )
                            st[i] = BOMB_V;
                    }

                    if( t == BOMB_H )
                    {
                        bombCount++;
                        sx[i] += 2;
                        if( sd[i]++ > 20 )
                            st[i] = BOMB_D;
                    }

                    if( t == EXPLODE )
                    {
                        if( sd[i]++ > 40 )
                        {
                            st[i] = 0;
                            if( i == 0 )
                            {
                                mode = MODE_RETRYLEVEL;
                                if( hitBase )
                                    level = 0;
                                else if( --ships <= 0 )
                                    mode = MODE_GAMEOVER;
                            }
                        }
                    }

                    if( t == METEOR )
                    {
                        sx[i] -= 3;
                    }

                    if( t == SAUCER )
                    {
                        float ang = 3.1415926f * ++sd[i] / 35;
                        sy[i] = (int)(126 - 34 * Math.sin(ang));
                        sx[i] = (int)(x + 224 - sd[i] + 5 * Math.sin(ang * 2));
                    }

                    if( t == SHIP )
                    {
                        sx[i] = x + shipx;
                        sy[i] = shipy;

                        g2.drawImage( sii[SHIP_FLAME], shipx - 22, shipy - 2, null );
                    }

                    
                    g2.drawImage( sii[t], sx[i] - x + siox[t], sy[i] + sioy[t], null );

                    if( t >= G_FLAT )
                    {
                        if( t < GT_FLAT )
                            g2.fillRect( sx[i] - x - 4, sy[i] + 4, 8, VIEW_BOT - sy[i] );
                        else
                            g2.fillRect( sx[i] - x - 4, VIEW_TOP, 8, sy[i] - 4 - VIEW_TOP);
                    }

                    if( sx[i] < x - 8 || sx[i] > x + 240 || sy[i] < VIEW_TOP )
                        st[i] = 0;
                }
            }

            if( mode == MODE_GAMEOVER )
            {
                g2.setColor( new Color(1f, 1f, 0f) );
                g2.drawString( "SCRAMBLE", 79, VIEW_MID - 15 );
                g2.drawString( "Game Over", 80, VIEW_MID );
                g2.drawString( "Press 1 to play", 70, VIEW_MID + 15 );
            }

            g2.setColor( new Color(0f, 0f, 1f) );
            g2.fillRect( 58, VIEW_BOT + 4, 100, 8 );
            g2.setColor( new Color(0.5f, 1f, 0f) );
            g2.fillRect( 58, VIEW_BOT + 4, fuel / 25, 8 );

            for( int i = 1; i < ships; i++ )
            {
                g2.drawImage( sii[SHIP], i * 20 - 16, VIEW_BOT + 13, null );
            }

            




            final int FRAME_PERIOD  = 16666667; 


            long    now = System.nanoTime();
            long elapsed = now - start;
            if( elapsed < -FRAME_PERIOD )
                start = now;
            else
            {

                try {
                    Thread.sleep( Math.max(0, (FRAME_PERIOD - elapsed) / 1000000) );
                } catch( Exception e ) {}
                start += FRAME_PERIOD;
            }
/*
            start += FRAME_PERIOD;
            do
            {
                Thread.yield();
            }
            while( System.nanoTime() - start < 0 );
*/
            appletG.drawImage( screen, 0, 0, null );

            if( !isActive() )
                return;

            
            

            count++;
        }
        } catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    public int addSprite(int[] st, int[] sx, int[] sy, int[] sd, int t, int x, int y )
    {
        final int G_FLAT    = 15;
        for( int i = t < G_FLAT ? 0 : 40; i < st.length; i++ )
        {
            if( st[i] == 0 )
            {
                st[i] = t;
                sx[i] = x;
                sy[i] = y;
                sd[i] = 0;
                return i;
            }
        }

        return -1;
    }

    @Override
    public void processEvent(AWTEvent e )
    {
        if( e.getID() == KeyEvent.KEY_PRESSED )
        {
            int code = ((KeyEvent)e).getKeyCode();
            m_keysDown[code] = true;
        }

        if( e.getID() == KeyEvent.KEY_RELEASED )
        {
            int code = ((KeyEvent)e).getKeyCode();
            m_keysDown[code] = false;
        }
/*
        if( e.getID() >= MouseEvent.MOUSE_FIRST &&
            e.getID() <= MouseEvent.MOUSE_LAST )
        {

            m_mouseEvent = (MouseEvent)e;
        }*/
    }

    private final boolean[]           m_keysDown = new boolean[1024];
    
}
