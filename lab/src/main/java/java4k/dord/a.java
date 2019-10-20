package java4k.dord;

/*
 * Dord
 * Copyright (C) 2013 meatfighter.com
 *
 * This file is part of Dord.
 *
 * Dord is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dord is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http:
 *
 */

import java4k.GamePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class a extends GamePanel {

  
  private final boolean[] a = new boolean[65535];
  
  public a() {
    super(true);
    new Thread(this).start();
  }

  @Override
  public void run() {
    
    final var S
        = "aaaaabbbbabbbbabbbbabbbbaaaaaaabbbbabbbbabbbbabbbbccccccccccaaacccca"
        + "ccccaccccaccccaccccaaaccccccccccddddddddddaaaddddaddddaddddaddddaddd"
        + "daaaddddddddddeeeeeeeeeeaaaeeeeaeeeeaeeeeaeeeeaeeeeaaaeeeeeeeeeeaaaa"
        + "aafaffaaaaaffffffaaaffaaaffffffaaaaaffafaaaaaaaaaaaagaggaaaaagggggga"
        + "aaggaaaggggggaaaaaggagaaaaaahhaaahaiaiagaggagaggaaaaaaiaiihaiaghhaia"
        + "hhhaihhhhahhhhahahhahhahahhhaaaaaaaaaaaahhhaahhahahahhahhhhaccccccch"
        + "hchhhhchhccchccccccchhcchhhccchhhcccchhcccdddddddhhdhhhhdhhdddhddddd"
        + "ddhhddhhhdddhhhddddhhdddeeeeeeehhehhhhehheeeheeeeeeehheehhheeehhheee"
        + "ehheeeggggggggggggggggggggggggggggggggggggggggggggggggggjjjjjkkkkjkk"
        + "kkjkkkkjkkkkjjjjjjjkkkkjkkkkjkkkkjkkkkhhcchhciicccaachcaachcaacccccc"
        + "hchcchchchhchchchhchhhcchcciichcaachcaacccaachcccchchcchchccchhchchc"
        + "hhhhddhhdiidddaadhdaadhdaaddddddhdhddhdhdhhdhdhdhhdhhhddhddiidhdaadh"
        + "daadddaadhddddhdhddhdhdddhhdhdhdhhhheehheiieeeaaeheaaeheaaeeeeeehehe"
        + "ehehehhehehehhehhheeheeiieheaaeheaaeeeaaeheeeeheheeheheeehhehehehhhh"
        + "hhahahhahhahahhhaaaaaaaaaaaahhhaahhahahahhahhhhahhahahhahahhhaaaahaa"
        + "hhaaahhaaaaahaahhhaahhahahhahahhffhhfiifffaafhfaafhfaaffffffhfhffhfh"
        + "fhhfhfhfhhfhhhffhffiifhfaafhfaafffaafhffffhfhffhfhfffhhfhfhfhhchhchh"
        + "chchhchchhchccccccchcaachcaacccaachciichhcchchchhchhchhchcchchcchccc"
        + "cccaachcaachcaaccciichhcchhhbbhhbiibbbaabhbaabhbaabbbbbbhbhbbhbhbhhb"
        + "hbhbhhbhhhbbhbbiibhbaabhbaabbbaabhbbbbhbhbbhbhbbbhhbhbhbhhhhllhhliil"
        + "llaalhlaalhlaallllllhlhllhlhlhhlhlhlhhlhhhllhlliilhlaalhlaalllaalhll"
        + "llhlhllhlhlllhhlhlhlhhhmmmmmmmmmmaiammaaammaaammmmmmmmmamhmmmahhmmmh"
        + "mmmhhmmmmmmmmmmaiammaaammaaammmmmmmmmamhmmmahhhmmhhmmmhmmmhhhmmmhmmm"
        + "ammmammmmmmmaaammaaammaiammmmmmhmmmmhhmmmhhhmmhmmmammmammmmmmmaaamma"
        + "aammaiammmmmmhmmmm"   
    
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggghgggghggggg"
        + "ggggaaaaaaaagggg"  
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggggaggggggggg"
        + "ggggggaggggggggg"
        + "ghgzggaghgggghgg"
        + "aaaaaaagggaaaaaa"
            
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gghggggggggggggg"
        + "ggaggggggggggggg" 
        + "ggggggaggggggggg"
        + "ggggggggggaggggg"
        + "ggggggggggggggag"
        + "ggaggggggggggggg"
        + "ggggggaggggggggg"
        + "ggggggggggaggggg"
        + "ggggggggggggggzg"
        + "iiiiiiiiiiiiiaaa"
            
        + "gggggggggggggggg"
        + "ggggjggggggggggg"
        + "gggaaagggggggggg"
        + "gggggggggggggggg" 
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggggggabbbaggg"
        + "ggggggggagggaggg"
        + "ggggggggagggaggg"
        + "gzggggggaghgaggg"
        + "aaagggaaaaaaaaaa"
            
        + "bhbgggggjggggggg"
        + "aaaaaaaaaaaaaaaa"            
        + "gggggggggggggggg"
        + "gggggggggggggggg" 
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "hggggggggggggggg"
        + "aggggggggggggggg"
        + "aggggggggggggggg"
        + "agggggggggggggga"
        + "agzgggggggggggga"
        + "aaaggggggggggbbb" 
    
        + "ggggggaggggggggg"
        + "ggggjgakgggzgggg"
        + "bbbbbbabbbbbbbbb"
        + "aaaaaaagggaaaaaa" 
        + "ggggggagggggggha"
        + "ggggggcgggggggga"
        + "ghggggcgggggggga"
        + "ggggggagggcccgga"
        + "aaaaaaagggggggga"
        + "gggaggggggggggga"
        + "ghgaggggggggggga"
        + "gggaggaaaaaaaaaa"
    
        + "abbgggggggggaggg"
        + "ahbggggggbggaggg"
        + "abbgggggggggaggg"
        + "agggggbgggggaggg" 
        + "aggggggzggggagkg"
        + "aaaaaadddaaagggg"
        + "aaglgaghgagjgggg"
        + "aacccaaaaabbbggg"
        + "gggggggggggggggg"
        + "gggbgggggggggggg"
        + "gggggggggggggggg"
        + "ghggggbgggggaggg"
            
        + "gggggggggggggggg"
        + "ggggghghghgggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg" 
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggmmmmmmmggggg"
        + "ggggmmmmmmmggggg"
        + "ggggmmmmmmmggggg"
        + "ggggmmmmmmmggggg"
        + "gzggmmmmmmmgghgg"
        + "aaaaaaaaaaaaaaaa"

        + "mmmmmmmmmmmmmmmm"
        + "mmmmmmmmmmmmmmmm"
        + "mmmmmmmmmmmmmmmm"
        + "mmmmmmmammmmmmmm" 
        + "gggggggzgggggggg"
        + "ghgghggagghgghgg"
        + "gggggggggggggggg"
        + "ghgghggggghgghgg"
        + "gggggggggggggggg"
        + "mmmmmmmmmmmmmmmm"
        + "mmmmmmmmmmmmmmmm"
        + "mmmmmmmmmmmmmmmm"            
            
        + "aaaaaaaaaaaaaaaa"
        + "maaaaammmmmmmmmm"
        + "maaaaammmmmmmmmm"
        + "mmmmmmmmmmmmmmmm" 
        + "mmmmmmmmmmmmmmmm"
        + "gggggggggggmmggg"
        + "ghggghgggggmmggg"
        + "gagggagggggmmggg"
        + "gagggagggggmmggg"
        + "gagggagggggmmggg"
        + "gaghgagggggmmgzg"
        + "aaaaaaaaaaammaaa" 

        + "gggggggggggggggg"
        + "gggggghqghgggggg"
        + "ggggaaaaaaaagggg"
        + "gggggggggggggggg"
        + "aaggggggggggggaa" 
        + "ggggghgggqhggggg"
        + "ggggaaaaaaaagggg"
        + "gggggggggggggggg"
        + "aaggggggggggggaa"
        + "gggghggqggghgggg"
        + "gzggaaaaaaaagggg"
        + "aaaaaaaaaaaaaaaa"
    
        + "gggggggggggggggg"
        + "ggggggggggaghggg"
        + "ggggggggggaaaaag"
        + "ggggggggggaggggg" 
        + "ggggagggggaggggg"
        + "ggggggghggaggggg"
        + "ggaaaaaaaaaggggg"
        + "gggggggggrggggaa"
        + "gggggggrggggggaa"
        + "gggggrggggggaaaa"
        + "gzgrggggggggaaaa"
        + "aaaaaaaaaaaaaaaa"   
    
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggghggqghggggg"            
        + "ggggaaaaaaaagggg"
        + "gggggggggggggggg"
        + "agggggggggggggga" 
        + "ggqggggggggggggg"
        + "aaaggggggggggaaa"        
        + "gggggggaaggggggg"
        + "qggggggggggggggg"        
        + "aaaaaggzgggaaaaa"
        + "aaaaaaaaaaaaaaaa"  
    
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggrgghggggggggg"
        + "ggggggaggggggggg" 
        + "gggggggggggggggg"
        + "gggaggggggggggag"
        + "ggggggggggggrggg"
        + "ggggggggggggaggg"
        + "ggggggggggrggggg"
        + "ggggggggggaggggg"
        + "ggggggggzggggggg"
        + "iiiiiiiaaaiiiiii"
                             
        + "gggggggggdgggggg"
        + "ggkgcggggdzglgqg"
        + "aaaaaagggaaaaaaa"
        + "gggggagggcggggbg"
        + "gghggagggcggggbg"
        + "aaaaaagggcghgaaa"
        + "gggggagggcgagggg"
        + "ggcgqagggcgggggg"
        + "aaaaaagggaaaaaaa"
        + "gggggagggagggggg"  
        + "gghggagggagqggjg"
        + "aaaggagggaaaaaaa"
    
        + "mmgggmmammggggga"
        + "mmgggmmammggggga"
        + "mmgggmmmmmggggga"
        + "mmgrgmmmmmgrggga" 
        + "mmghgmmmmmghggga"
        + "mmaaaaaaaaaaamma"
        + "mmggjggagbgggmma"
        + "mmgggggahbgggmma"
        + "mmgggggaaagggmma"
        + "mmgggggagggggmma"
        + "mmgzgggagggggmma"
        + "aaaaaaaaaaaaaaaa"
    
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ghgghgghggggghgg" 
        + "g4gg5gg6ggggg3gg"
        + "gggggggggghggggg"
        + "gggggggggg2ggggg"
        + "ggggggghgggggggg"
        + "gggghgg1gggggggg"
        + "gggg0ggggggggggg"
        + "gzgggggggggggggg"
        + "aaaiiiiiiiiiiiii"  
    
        + "gggggggggigggggi"
        + "gggggggggigggggi"
        + "ggggghgggigggggi"
        + "gggggggggigggggi" 
        + "gghggggggigggggi"
        + "ggggg5gggigggggi"
        + "gggggggggggggzgi"
        + "ggggggg4gggggagi"
        + "gggg3ggggggggggi"
        + "gggggggggggggggi"
        + "gg2gggg1ggg0gggi"
        + "iiiiiiiiiiiiiiii" 
    
        + "gggggggggggggggg"
        + "gg1ggggggggggggg"
        + "gggggggzgggggggg"
        + "gggg0ggagggggggg" 
        + "gggggggggggggggg"
        + "iiiiiiiiiiiiiiii"
        + "gggggggggggggghg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggggg3gggggg6g"
        + "gggggggggggggggg"
        + "gggg2ggg4ggg5ggg"
    
        + "iiiiiiiiiiiiiiii"
        + "gggggggggggggghg"
        + "gggggggggggggggg"
        + "gggggggggggggggg" 
        + "876gggggggggggh9"
        + "ggqggggggggggggg"
        + "gaaaaaaaaaaggg5g"
        + "gggggggggggggggg"
        + "gggzgggggggg4ggg"
        + "210aggggggggggg3"
        + "gggggggggggggggg"
        + "iiiiiiiiiiiiiiii"
            
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggggggggggggag"
        + "ggggggaaaaaggggg" 
        + "gggggggggggggggg"
        + "gggagggggggggggg"
        + "gggggggggggggggg"
        + "agggggggsggggggg"
        + "aaaaaaaaaagggggg"
        + "gggggggggggggggg"
        + "gggggggzggggaaaa"
        + "aaaaaaaaaaaaaghg"            
    
        + "gggggggggggggagg"
        + "gghggggggggggajg"
        + "aaaaaggggggggaaa"
        + "ggggaggggsgggggg" 
        + "ggggaaaaaaaagggg"
        + "gggggggggggagggg"
        + "aagggggggggaaaaa"
        + "gggggsgggggghggg"
        + "ggggaaaaaaaaaagg"
        + "gggggggbgggagggg"
        + "ggaggggbghgagzgg"
        + "aaaaaaaaaaaaaaaa" 
    
        + "ggggggggggggggga"
        + "gggggggggggggsga"
        + "ggggaaahhhhaaaaa"
        + "g5ggagggggggggga" 
        + "ggggagggggggggga"
        + "g24gasggggggggga"
        + "ggggahhhhaaaaaaa"
        + "g31gggggggggggga"
        + "ggggggggggggggga"
        + "g0ggggggggggggga"
        + "gggggggggzggggga"
        + "aaaaaaaaaaaaaaaa" 
    
        + "iiiiiiaaaaaaiiii"
        + "iggggigggmmmmmmi"
        + "igzggigggmmmmmmi"
        + "igaggigggmmmmmmi" 
        + "igggggghgggggmmi"
        + "iggggggggggggmmi"
        + "igggggghgggggmmi"
        + "iggggggggggggmmi"
        + "iggggggggggggmmi"
        + "iggggggggggggmmi"
        + "igggggghghgggmmi"
        + "iiiiiiaaaaaaaaai"
    
        + "igggggigggiiiiii"
        + "ighghgigggiggggi"
        + "igggggigggiggzgi"
        + "ighghgigggiggagi" 
        + "iggggggggggggggi"
        + "ighghggggggggggi"
        + "igggggggaggggggi"
        + "ighghggggggggggi"
        + "iggggggggggggggi"
        + "ighghggggggggggi"
        + "iggggggggggggggi"
        + "igggggggggiiiiii" 
    
        + "agggggggggrggggg"
        + "agggggggrggggghg"
        + "agggggrggggggggg"
        + "aggaaaaaaaaaaaaa" 
        + "gggsgghggggggggg"
        + "aaaaaaaaagggaaaa"
        + "ggggggggggggsggg"
        + "aaaggaaaaaaaaaaa"
        + "ggggggggqggggggg"
        + "aaaaaaaaaaggaaaa"
        + "gzggqggggggggggg"
        + "aaaaaaaaaaaaaaaa" 
    
        + "agghgggggggggggg"
        + "agggggggghgggghg"
        + "aggggggggggggggg"
        + "agggggeeeeeegggg" 
        + "aggggggggggggggg"
        + "aagggggggggggggg"
        + "aggggggggggggggg"
        + "agggffffffffgggg"
        + "aggggggggggggggg"
        + "agggggggggggggaa"
        + "agzgggggggggggaa"
        + "aaaaeeeeeeeeaaaa"
    
        + "gggggggggggggghi"
        + "ggggggeeeeeeeeee"
        + "gggggagggggggggg"
        + "igggaggggggggggg" 
        + "iggagggggggggggg"
        + "igggggggiggggggg"
        + "fffffffffffggggg"
        + "ggggggggggggggga"
        + "gggggggggggggggi"
        + "ggggggggggggaggi"
        + "gzggggggiggggggi"
        + "eeeeeeeeeeeeeeee"  
    
        + "gggggggggggggggg"
        + "gggghggghgghgggg"
        + "ghgggggggggggggg"
        + "gggggggggggggggg" 
        + "ggggfgggfggfgggg"
        + "ggggggggggggggeg"
        + "gggggggggggggggg"
        + "gegggggggggggggg"
        + "gggggggggggggggg"
        + "ggggegggggeggggg"
        + "gggggggeggggggzg"
        + "iiiiiiiiiiiiiaaa" 
    
        + "iggggggggggggggi"
        + "iggggggggggaaaig"
        + "iggggggggqgggigg"
        + "iggggggfffffiggg" 
        + "iggggggggggigggg"
        + "ifffggggggigghgg"
        + "iggggggggigggggg"
        + "iggggg1giggggggg"
        + "iggggggigggggghg"
        + "iggg0giggggg3ggg"
        + "igzggigggggggggg"
        + "iaaaigggggg2gggg" 
    
        + "gggggrgggggggrgg"
        + "grgggggggggzgggk"
        + "cccccccccccccccc"
        + "ggghggghggghgggg" 
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggl"
        + "dddddddddddddddd"
        + "bbbbbbbbbbbbbbbb"
        + "ghggghggghggghgg"
        + "aaaaaaaaaaaaaaaa"
        + "gggjgggggrgggggg" 
    
        + "tttgggtttttgggtt"
        + "gggggggggggggggg"
        + "gggghggggggghggg"
        + "ggaaaaaaaaaaaaaa" 
        + "gggggggggggggggg"
        + "aggggggggggggggg"
        + "aggghggggggghggg"
        + "aaaaaaaaaaaaaggg"
        + "gggggggggggggggg"
        + "ggggggggggggggga"
        + "ggggzggggggghgga"
        + "aaaaaaaaaaaaaaaa" 
    
        + "gggggggggttttttt"
        + "gggghggggggggggg"
        + "ghgggggggggghggg"
        + "aagggggggggggggg" 
        + "gggggggghggggghg"
        + "ggggggggaaaaaaaa"
        + "ggghgggggggggggg"
        + "gggaaggggggggggg"
        + "gggggghggggggggg"
        + "ggggggaggggggggg"
        + "ghgggggggghgggzg"
        + "tttttttgggaaaaaa"
    
        + "gggggggggggghggg"
        + "gggggghggggggggg"
        + "ggggggggggggiggg"
        + "ggghggaggauggggg" 
        + "gggggggggggggggg"
        + "gggaugggggggggag"
        + "gggggggggggggggg"
        + "ggggggggggaugggg"
        + "gggggggggggggggg"
        + "ggggaaaggggggggg"
        + "gzggaaaggggggggg"
        + "aaaaaaaaaaaaaaaa" 
    
        + "ggggggggghgggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggggggggg8gg7g" 
        + "gggggggggggggg3g"
        + "g6gggggggggggggg"
        + "gggg5gg4gggggggg"
        + "ggggggggggaugggg"
        + "g1ggggggggggg2gg"
        + "gggg0ggggggggggg"
        + "gggggggzgggggggg"
        + "iiiiiiaaaaaaaaaa"
    
        + "aggggggggggggggg"
        + "aghgghgggggggggg"
        + "aggggggggggggggg"
        + "aggggggggggggggg" 
        + "agaggggggggggggg"
        + "aggggggghggggggg"
        + "aggggaugaggauggg"
        + "agggggggggggggga"
        + "agggggggggggggga"
        + "aggggigggggiggaa"
        + "agzggigghggiggaa"
        + "aaaaaaaaaaaaaaaa"  
    
        + "aggggggggggggggg"
        + "aggggggggggggggg"
        + "aggghqgggggghggg"
        + "agggaaaaaaaaaggg" 
        + "aggghggggggvhggg"
        + "aagggggggggggggg"
        + "aggghqgggggghggg"
        + "agggaaaaaaaaaggg"
        + "aggghggggggvhggg"
        + "agggggggggggggga"
        + "agzggggggggggggg"
        + "aaaaaaaaaaaaaaaa"   
    
        + "aaaaaaaaaaaaaaaa"
        + "ammmmmmvmmmmmmaa"
        + "ammmmmmmmmmmmmaa"
        + "ammmmmmmmmmmmmma" 
        + "ammmmmmmmmmmmmma"
        + "ammaaaaaaaaammmm"
        + "ammaaammmmvmmmmm"
        + "ammaaammmmmmmmmm"
        + "ammaammmmmmmmmmm"
        + "ammaammmmmmmmmmm"
        + "aggagggggggggggg"
        + "azgagggggggggghg" 
    
        + "aggggggggggggggg"
        + "ahggggggggggzggg"
        + "affffffffffffggg"
        + "aggggggggggggggg" 
        + "aggggggggggggggh"
        + "agggggeeeeeeeeee"
        + "aggggggggggggggg"
        + "ahgggggggggggggg"
        + "affffffffffffggg"
        + "ahgggggggggggggg"
        + "aeeeeeeeeeeeeeee"
        + "ttttttgghgtttttt" 
    
        + "aaaaaaaaaaaaaaaa"
        + "mmzmmmmmmmmmmaaa"
        + "mmmmmmmmmmmmmaaa"
        + "mmmmmmmmmmmmmmaa" 
        + "mmmmmmmmmmmmmmaa"
        + "mmmmmmmmmmmmmmma"
        + "aaaaaaammmaaamma"
        + "mmmvmmmmmmmmmmma"
        + "mammmammmammmmma"
        + "mmmmmmmmmmmmmmma"
        + "dddabbbagggcccca"
        + "gkgaglgagjgcghga"
    
        + "aggggggggggggggg"
        + "agggggggggggghgg"
        + "aggggaaaaaaaaaaa"
        + "aggggggggggggggg" 
        + "aagggggggggggggg"
        + "aaaggggggggggggg"
        + "aggggggggggggggg"
        + "aggggaggggawgggg"
        + "aggggggggggggggg"
        + "agggggggggggggga"
        + "azggggggggggggga"
        + "aaaaaaaaaaaaaaaa" 
    
        + "gggggggaaggggggg"
        + "gggggggaagggzggg"
        + "aaaaiiiaaaaaaaaa"
        + "mmmmgggagggmmmmm" 
        + "mmmmgggahggmmmmm"
        + "gggggggagggggggg"
        + "ggggggaagggggggg"
        + "gggggaaagggggggg"
        + "ggggaaaagggggggg"
        + "ggqgghgagggggggg"
        + "eeeegggagggeeeee"
        + "gggggggagggggggg"  
    
        + "ggggggggggggggga"
        + "hgggggggggggggga"
        + "ggggggggggggggga"
        + "ggggggggggggggga" 
        + "ggggggggggggggga"
        + "agggggggagggggga"
        + "ggggggggggggggga"
        + "ggggawgggggaugga"
        + "ggggggggggggggga"
        + "gggggggggggaugaa"
        + "zgggggggggggggaa"
        + "aaaaaaaaaaaaaaaa" 
    
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg" 
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggnnnnnnnnnggg"
        + "ggggnnnnnnnnnggg"
        + "ggggnnnnnnnnnggg"
        + "ggggnnngggnnnggg"
        + "gzggnnnghgnnnggg"
        + "aaaaaaaaaaaaaaaa" 
    
        + "gggggggggggghggg"
        + "gggggggggggggggg"
        + "ggggggggggnggggg"
        + "gggggggngggggggg" 
        + "ggggnggggggggggg"
        + "gngggggggggggggg"
        + "ggggggggggggggng"
        + "gggggggggggngggg"
        + "ggggggggnggggggg"
        + "gggggngggggggggg"
        + "ggnggggggggggzgg"
        + "aaaaaaaaaaaaaaaa"  
    
        + "ggggggqgggggaggg"
        + "ggggggaaaaaanggg"
        + "ggggggagngggnggg"
        + "ggaaaaagngggnggg" 
        + "ggagngggngggngrg"
        + "aaagngggngggaaaa"
        + "ggggngggngggagla"
        + "ggggngggaaaaagga"
        + "zgggngggagggggga"
        + "ddggaaaaagggggna"
        + "hdggagggggggggna"
        + "aaaaagggggggaaaa"  
    
        + "agggggagggaggggh"
        + "agggzgagggaggggg"
        + "aggaaaagggcggggg"
        + "aggggjagggcggggn" 
        + "agggggagggaggggg"
        + "agggggagggaggngg"
        + "agggnnagggaggggg"
        + "anggngctttangggg"
        + "agggngcgggaaaaaa"
        + "bggnngagggaggggg"
        + "bggnngagggagkggg"
        + "aaaaaaagggaaaaaa"
    
        + "ggggggggggggggga"
        + "ggggggggggggggga"
        + "gggggggggggggzga"
        + "gggggaaaaaaaaaaa"
        + "gggggggggggghgga"
        + "ggggggggggggggga"
        + "ggggggggggggggga"
        + "ggggggggggggggna"
        + "ggggggggggggggga"
        + "gggggggggggnggga"
        + "ggggggggggggggga"
        + "iiiiiiiawgggggga" 
    
        + "ghgggggggggggggg"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggggggggggggng" 
        + "gggggggggggngggg"
        + "ggggggnggrgggggg"
        + "gggngggggggggggg"
        + "nggggggggggggggg"
        + "ggggggggggggnggg"
        + "gggrgggggngggggg"
        + "gggggggzgggggggg"
        + "aaaaaaaaaaaaaaaa"
    
        + "gggggaatttaaggga"
        + "gggggaagggaagggg"
        + "ghgggaagggaagggg"
        + "gggggaagggaaggga" 
        + "ggghgaagggggggga"
        + "gggggaagggggggga"
        + "gggggaagggaaggga"
        + "gggghaagggaaggga"
        + "gggggaagggaattta"
        + "ggggggggggaaggga"
        + "gggggzggggaaggga"
        + "iiiiiaagggaaggga"
    
        + "aggggggggggggggg"
        + "aggggggggggggggg"
        + "aggggggggggggggg"
        + "aggggggggggggggg" 
        + "aghggigghggggggg"
        + "aaaaaaaaaaaaaggg"
        + "aggggxggxggxgggg"
        + "agggggggggggggga"
        + "agggggggggggggga"
        + "agggggggggggggaa"
        + "agzgggggggggggaa"
        + "aaaaaaaaaaaaaaaa"
    
        + "gggggggggggggggg"
        + "ggggggggggggghgg"
        + "gggggggggggggggg"
        + "ggggggggaaaggggg" 
        + "gggggggggxgggggg"
        + "ggggnggggggggggg"
        + "gggggggggggggggg"
        + "ggagggawgggggggg"
        + "gggggggggggggggg"
        + "ggggggggggaggggg"
        + "ggggggggggagggzg"
        + "aaaaaaaaaaaaaaaa"
            
        + "aaaaaaaaaaaaaaaa"
        + "amzmxmxmxmxmmmaa"
        + "ammmmmmmmmmmmmaa"
        + "ammmmmmmmmmmmmma" 
        + "ammmmmmmmmmmmmmm"
        + "ammmaaaaaaaaammm"
        + "ammmmmmmmmmmmmmm"
        + "anmmmmmmmmmmmmmm"
        + "ammmmmmmmmmmmmmm"
        + "ammmmaaaaaaaaaaa"
        + "aggggggggggggggg"
        + "agggggghgghgghgg" 
    
        + "aggaxxaaaaaaaaaa"
        + "gggaggagggggggga"
        + "ggaaggagggggggha"
        + "ggggggagggggddda" 
        + "azggggaggggcdhda"
        + "aaaaggagggggddda"
        + "nggaggacgggggggc"
        + "nggagggggggggggc"
        + "nglaggggggaggggc"
        + "aaaabbagggggggga"
        + "gggakgaaaaaaaaaa"
        + "gggaaaajgggggggg" 

        + "aggggggammmagggg"
        + "aggggggammmagggg"
        + "agggeggammmagggg"
        + "aggggggamzmaghgg" 
        + "aegggggammmagggg"
        + "agggghgammmagggg"
        + "aaaaaaaaaaaaaaaa"
        + "ggghgggggggagggg"
        + "gggggggggggagggg"
        + "gggggggggggagghg"
        + "gggfgggggggagggg"
        + "gggggggigggagggg"             
            
        + "iiiiiiiiiiiiiiii"
        + "gggggggggggggggg"
        + "gggggggggggggggg"
        + "ggggghgggggigggg" 
        + "gggggawhgggigggg"
        + "gggggggaghgigggg"
        + "gg7ggggggauigggg"
        + "gggggggggggggg6g"
        + "g5gggggggggggggg"
        + "ghg4hgghggghgggg"
        + "ggggggggggggggzg"
        + "g3gg2gg1ggg0ggag"
            
        + "iggggggggggqgggi"
        + "igaaagagagaaaggi"
        + "iggaxgaragaggggi"
        + "iggaggaaagaauggi" 
        + "iggaggagagaggggi"
        + "iggaggagagaaaggi"
        + "igjgggggggggzggi"
        + "igaaagaggagbbggi"
        + "igavggaagagbgbgi"
        + "igaaggagaagbgbgi"
        + "igagsgaggagbhbgi"
        + "igaaagaggagbbggi";    
    
    final var TILES = 34;
      final var TILE_GREEN_BRICK = 2;
    final var TILE_BLUE_BRICK = 3;

      final var TILE_EMPTY = 6;
      final var TILE_GREEN_KEY = 10;

      final var TILE_RED_ENEMY_1 = 15;
    final var TILE_GREEN_ENEMY_0 = 16;
    final var TILE_GREEN_ENEMY_1 = 17;
    final var TILE_BLUE_ENEMY_0 = 18;
    final var TILE_BLUE_ENEMY_1 = 19;
    final var TILE_SPIKES_ENEMY_0 = 20;
    final var TILE_SPIKES_ENEMY_1 = 21;
    final var TILE_YELLOW_ENEMY_0 = 22;
    final var TILE_YELLOW_ENEMY_1 = 23;
    final var TILE_FLIPPED_RED_ENEMY_0 = 24;
    final var TILE_FLIPPED_RED_ENEMY_1 = 25;
    final var TILE_ORANGE_ENEMY_0 = 26;
    final var TILE_ORANGE_ENEMY_1 = 27;
    final var TILE_MAGENETA_ENEMY_0 = 28;
    final var TILE_MAGENETA_ENEMY_1 = 29;
      final var TILE_PLAYER_1 = 31;
    final var TILE_PLAYER_2 = 32;
    final var TILE_PLAYER_3 = 33;

      final var CHAR_SPIKES = 'i';
      final var CHAR_APPEARING_BLOCK = 'n';
      final var CHAR_GREEN_ENEMY = 'r';
    final var CHAR_BLUE_ENEMY = 's';
    final var CHAR_SPIKES_ENEMY = 't';
    final var CHAR_YELLOW_ENEMY = 'u';
    final var CHAR_FLIPPED_RED_ENEMY = 'v';

      final var FRAME_WIDTH = 160;
    final var FRAME_HEIGHT = 120;

      final var ENEMY_TYPE_RED = 0;

    var tiles = new BufferedImage[TILES];
    var imageBuffer = new BufferedImage(
        FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
    var g = imageBuffer.createGraphics();

    var i = 0;
    var j = 0;
    var k = 0;
    var x = 0;
    int y;


      for(; i < TILES; i++) {
      tiles[i] = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR_PRE);
      if (i != TILE_EMPTY) {
        for(y = 0; y < 10; y++) {
          for(x = 0; x < 5; x++) {
            k = S.charAt(j++);
              switch (k) {
                  case 'a':
                      k = 0xff000000;
                      break;
                  case 'b':
                      k = 0xffff6a00;
                      break;
                  case 'c':
                      k = 0xffff0000;
                      break;
                  case 'd':
                      k = 0xff00ff00;
                      break;
                  case 'e':
                      k = 0xff0000ff;
                      break;
                  case 'f':
                      k = 0xffffff00;
                      break;
                  case 'g':
                      k = 0xff00ffff;
                      break;
                  case 'h':
                      k = 0;
                      break;
                  case 'i':
                      k = 0xffffffff;
                      break;
                  case 'j':
                      k = 0xff6675cc;
                      break;
                  case 'k':
                      k = 0xff7f92ff;
                      break;
                  case 'l':
                      k = 0xffff00ff;
                      break;
                  default:
                      k = 0xfff7d3b3;
                      break;
              }
            tiles[i].setRGB(x, y, k);
            tiles[i].setRGB(9 - x, y, k);
          }
        }
      }
    }
    
    long nextFrameStartTime = 0;
      float playerVy = 0;
      float playerY = 0;
    var arrowTime = 0;
    var playerX = 0;
    var playerSpriteOffset = 0;
    var counter = 0;
    var level = 0;
    var resetDelay = 1;
    var blockDelay = 0;
    var blockIndex = 0;
    var appearingBlocks = 0;
    var diamonds = 0;
    var advanceLevel = 0;
    var blockClear = false;
    var keysReleased = false;
      Graphics2D g2 = null;
    final var MAP_HEIGHT = 12;
    final var MAP_WIDTH = 16;
    var map = new int[MAP_HEIGHT][MAP_WIDTH];
    var blocks = new int[10][2];
    var enemies = new ArrayList<int[]>();
    var BACKGROUND_COLOR = new Color(0x7F92FF);
      final var YELLOW_RADIUS = 10;
      final var APPEARING_BLOCK_DELAY = 50;
      final var ADVANCE_LEVEL_DELAY = 25;
      final var MAX_VY = 3f;
      final var JUMP_VY = -1.6f;
      final var GRAVITY = 0.05f;
      final var LEVELS = 56;
      final var ENEMY_TYPE_MAGENETA = 7;
      final var ENEMY_TYPE_ORANGE = 6;
      final var ENEMY_TYPE_FLIPPED_RED = 5;
      final var ENEMY_TYPE_YELLOW = 4;
      final var ENEMY_TYPE_SPIKES = 3;
      final var ENEMY_TYPE_BLUE = 2;
      final var ENEMY_TYPE_GREEN = 1;
      final var ENEMY_RADIUS = 6;
      final var ENEMY_CY = 5;
      final var ENEMY_CX = 4;
      final var ENEMY_TYPE = 3;
      final var ENEMY_V = 2;
      final var ENEMY_Y = 1;
      final var ENEMY_X = 0;
      final var SCREEN_HEIGHT = 600;
      final var SCREEN_WIDTH = 800;
      final var KEY_X = 120;
      final var KEY_R = 114;
      final var KEY_P = 112;
      final var KEY_N = 110;
      final var KEY_RIGHT = 1007;
      final var KEY_LEFT = 1006;
      final var CHAR_MAGENETA_ENEMY = 'x';
      final var CHAR_ORANGE_ENEMY = 'w';
      final var CHAR_RED_ENEMY = 'q';
      final var CHAR_PLAYER = 'z';
      final var CHAR_ANTIGRAVITY = 'm';
      final var CHAR_DIAMOND = 'h';
      final var CHAR_EMPTY = 'g';
      final var TILE_PLAYER_0 = 30;
      final var TILE_RED_ENEMY_0 = 14;
      final var TILE_APPEARING_BLOCK = 13;
      final var TILE_ANTIGRAVITY = 12;
      final var TILE_BLUE_KEY = 11;
      final var TILE_RED_KEY = 9;
      final var TILE_SPIKES = 8;
      final var TILE_DIAMOND = 7;
      final var TILE_LEFT_BRICK = 5;
      final var TILE_RIGHT_BRICK = 4;
      final var TILE_RED_BRICK = 1;
      final var TILE_BRICK = 0;
      while(true) {

          int[] enemy;
          do {
        nextFrameStartTime += 10000000;


              int p;
              if (resetDelay > 0) {
          if (--resetDelay == 0) {
            playerVy = 0;
            level = (level + advanceLevel + LEVELS) % LEVELS;
            
            
            enemies.clear();
            blockClear = false;
            diamonds = 0;
            appearingBlocks = 0;
            blockIndex = 0;
            blockDelay = APPEARING_BLOCK_DELAY;
            j = 50 * (TILES - 1) + level * 192;
            for(y = 0; y < MAP_HEIGHT; y++) {
              for(x = 0; x < MAP_WIDTH; x++) {
                p = k;
                k = S.charAt(j++);
                if (k <= '9') {
                  blocks[k - '0'][0] = x;
                  blocks[k - '0'][1] = y;
                  appearingBlocks++;
                  k = CHAR_EMPTY;
                } else if (k == CHAR_PLAYER) {
                  
                  playerX = 10 * x;
                  playerY = 10 * y;
                  k = p == CHAR_ANTIGRAVITY 
                      ? CHAR_ANTIGRAVITY : CHAR_EMPTY;                 
                } else if (k >= CHAR_RED_ENEMY) {
                  for(i = 0; i < (k == CHAR_ORANGE_ENEMY ? 3 : 1); i++) {
                    
                    enemy = new int[8];
                    enemy[ENEMY_X] = 10 * (x + i);
                    enemy[ENEMY_Y] = 10 * y;
                    enemy[ENEMY_V] = k == CHAR_MAGENETA_ENEMY ? 0 : 1;
                    enemy[ENEMY_TYPE] = k - CHAR_RED_ENEMY;
                    enemy[ENEMY_RADIUS] = YELLOW_RADIUS * (i + 1);
                    enemy[ENEMY_CX] = enemy[ENEMY_X] - enemy[ENEMY_RADIUS];
                    enemy[ENEMY_CY] = enemy[ENEMY_Y];                    
                    enemies.add(enemy);
                  }                                    
                  k = p == CHAR_ANTIGRAVITY 
                      ? CHAR_ANTIGRAVITY : CHAR_EMPTY; 
                }
                if (k == CHAR_DIAMOND) {
                  diamonds++;
                }
                map[y][x] = k - 'a';
              }
            }
            nextFrameStartTime = System.nanoTime();
          } else {
            continue;
          }
        }
        
        
        if (appearingBlocks > 0 && --blockDelay == 0) {          
          blockDelay = APPEARING_BLOCK_DELAY;
          if (blockClear) {
            i = (blockIndex - 2 + appearingBlocks) % appearingBlocks;          
            map[blocks[i][1]][blocks[i][0]] = TILE_EMPTY;
          } else {
            map[blocks[blockIndex][1]][blocks[blockIndex][0]] = TILE_BRICK;
            blockIndex = (blockIndex + 1) % appearingBlocks;          
          }
          blockClear = !blockClear;
        }
            var insideOfBrick = false;
        for(i = 0; i < 2; i++) {
          for(j = 0; j < 2; j++) {
            if (map[((9 * i + ((int)playerY) + FRAME_HEIGHT) 
                % FRAME_HEIGHT) / 10][((9 * j + playerX + FRAME_WIDTH) 
                % FRAME_WIDTH) / 10] <= TILE_LEFT_BRICK) {
              insideOfBrick = true;
            } 
          }
        }


            var px = playerX;
            var py = (int)playerY;
            var b = IntStream.of(KEY_X, KEY_R, KEY_N, KEY_P).noneMatch(v -> a[v]);
        if (b) {
          keysReleased = true;
        }
        if (keysReleased && a[KEY_R]) {
          keysReleased = false;
          advanceLevel = 0;
          resetDelay = 1;
        }
        if (keysReleased && a[KEY_N]) {
          keysReleased = false;
          advanceLevel = 1;
          resetDelay = 1;
        }
        if (keysReleased && a[KEY_P]) {
          keysReleased = false;
          advanceLevel = -1;
          resetDelay = 1;
        }
        playerSpriteOffset = 0;
            var floorY = 10;
            var vk = 1;
        if (map[((5 + ((int)playerY) + FRAME_HEIGHT) % FRAME_HEIGHT) / 10]
                 [((5 + playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10] 
                      == TILE_ANTIGRAVITY) {
          
          floorY = -1;
          vk = -1;
          if (playerVy == 0) {
            playerSpriteOffset = 2;
          }
        }
        if ((counter & 1) == 1) {
          
          i = map[((10 + ((int)playerY) + FRAME_HEIGHT) % FRAME_HEIGHT) / 10]
                 [((playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10];
          j = map[((10 + ((int)playerY) + FRAME_HEIGHT) % FRAME_HEIGHT) / 10]
                 [((9 + playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10];
          x = playerX;
          if (i == TILE_LEFT_BRICK || j == TILE_LEFT_BRICK) {
            playerX--;
          }
          if (i == TILE_RIGHT_BRICK || j == TILE_RIGHT_BRICK) {
            playerX++;
          }
          for(i = 0; i < 2; i++) {
            for(j = 0; j < 2; j++) {
              if (map[((9 * i + ((int)playerY) + FRAME_HEIGHT) 
                  % FRAME_HEIGHT) / 10][((9 * j + playerX + FRAME_WIDTH) 
                  % FRAME_WIDTH) / 10] <= TILE_LEFT_BRICK) {
                playerX = x;
              } 
            }
          }
        }

            var remainderVy = 0;
              int q;
              int z;
              for(k = 0; k < 2 || remainderVy != 0; k++) {
          x = playerX;
                var Y = playerY;
          if (k == 0) {
            if (a[KEY_LEFT]) {
              arrowTime++;
              playerX--;              
            } else if (a[KEY_RIGHT]) {
              arrowTime++;
              playerX++;
            } else {
              arrowTime = 0;
            }
          } else {
            if (vk * playerVy < 0        
              || (map 
                  [((floorY + ((int)playerY) + FRAME_HEIGHT) % FRAME_HEIGHT) / 10]
                  [((playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10] 
                      > TILE_LEFT_BRICK
              && map 
                  [((floorY + ((int)playerY) + FRAME_HEIGHT) % FRAME_HEIGHT) / 10]
                  [((9 + playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10] 
                      > TILE_LEFT_BRICK)
              || insideOfBrick) {
              
              if (k == 1) {                
                playerVy += vk * GRAVITY;
                if (playerVy > MAX_VY) {
                  playerVy = MAX_VY;
                } else if (playerVy < -MAX_VY) {
                  playerVy = -MAX_VY;
                }
                playerY += playerVy % 1;
                remainderVy = (int)(playerVy - (playerVy % 1)); 
              } else if (remainderVy > 0) {
                playerY++;
                remainderVy--;
              } else {
                playerY--;
                remainderVy++;
              }                  
            } else {
              playerVy = 0;
              remainderVy = 0;
              if (keysReleased && a[KEY_X]) {                
                keysReleased = false;                
                playerVy = vk * JUMP_VY;
              }
            }
          }
          playerX = (playerX + FRAME_WIDTH) % FRAME_WIDTH;
          playerY = (playerY + FRAME_HEIGHT) % FRAME_HEIGHT;
          for(i = 0; i < 2; i++) {
            for(j = 0; j < 2; j++) {
              var mx = ((9 * j + playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10;
              var my = ((9 * i + ((int)playerY) + FRAME_HEIGHT)
                  % FRAME_HEIGHT) / 10;
              z = map[my][mx];
              if (z <= TILE_LEFT_BRICK) {
                if (!insideOfBrick) {
                  
                  playerX = x;
                  playerY = Y;
                  if (k > 0 && vk * playerVy < 0) {
                    playerVy = -playerVy;
                  }    
                }
              } else if (z == TILE_DIAMOND) {
                map[my][mx] = TILE_EMPTY;
                diamonds--;
              } else if (z == TILE_SPIKES) {                
                resetDelay = 1;
                advanceLevel = 0;
              } else if (z >= TILE_RED_KEY && z <= TILE_BLUE_KEY) {
                map[my][mx] = TILE_EMPTY;
                for(p = 0; p < MAP_HEIGHT; p++) {
                  for(q = 0; q < MAP_WIDTH; q++) {
                    if (map[p][q] == z + TILE_RED_BRICK - TILE_RED_KEY) {
                      map[p][q] = TILE_EMPTY;
                    }
                  }
                }
              }
            }
          }
        }
        
        
        for(i = 0; i < 2; i++) {
          for(j = 0; j < 2; j++) {
            p = ((9 * j + px + FRAME_WIDTH) % FRAME_WIDTH) / 10;            
            q = ((9 * i + py + FRAME_HEIGHT) % FRAME_HEIGHT) / 10;
            if (map[q][p] == TILE_APPEARING_BLOCK) {
              var blockUnfound = true;
              for(y = 0; y < 2; y++) {
                for(x = 0; x < 2; x++) {
                  if (p == ((9 * x + playerX + FRAME_WIDTH) % FRAME_WIDTH) / 10
                      && q == ((9 * y + ((int)playerY) 
                          + FRAME_HEIGHT) % FRAME_HEIGHT) / 10) {
                    blockUnfound = false;
                    break;
                  }
                }
              }
              if (blockUnfound) {                
                map[q][p] = TILE_BRICK;
              } 
            }
          }
        }
        
        
        counter++;
        for(i = 0; i < enemies.size(); i++) {
          enemy = enemies.get(i);
          if ((counter & 1) == 1
              || (enemy[ENEMY_TYPE] == ENEMY_TYPE_BLUE 
                  && enemy[ENEMY_Y] == (int)playerY)
              || enemy[ENEMY_TYPE] == ENEMY_TYPE_MAGENETA) {
            
            if (enemy[ENEMY_TYPE] == ENEMY_TYPE_MAGENETA) {
              
              if (enemy[ENEMY_Y] % 10 == 0) {                
                if (enemy[ENEMY_V] == 0) {
                  x = playerX - enemy[ENEMY_X];
                  if (x < 0) {
                    x = -x;
                  }
                  if (x % FRAME_WIDTH < 18) {
                    enemy[ENEMY_V] = 1;
                  }
                }                
                x = enemy[ENEMY_X] / 10;
                y = ((enemy[ENEMY_Y] / 10) 
                    + enemy[ENEMY_V] + MAP_HEIGHT) % MAP_HEIGHT;
                z = map[(y + MAP_HEIGHT) % MAP_HEIGHT][x];                
                if (z != TILE_EMPTY && z != TILE_ANTIGRAVITY 
                    && z != TILE_APPEARING_BLOCK) {                  
                  enemy[ENEMY_V] = enemy[ENEMY_V] == 1 ? -1 : 0;
                }
              }
              if (enemy[ENEMY_V] == 1 || (counter & 3) == 3) {
                enemy[ENEMY_Y] = (enemy[ENEMY_Y] + enemy[ENEMY_V] 
                    + FRAME_HEIGHT) % FRAME_HEIGHT;
              }
            } else if (enemy[ENEMY_TYPE] == ENEMY_TYPE_YELLOW
                || enemy[ENEMY_TYPE] == ENEMY_TYPE_ORANGE) {
              
              float d = enemy[ENEMY_TYPE] == ENEMY_TYPE_ORANGE ? 16 : 8;
              enemy[ENEMY_X] = enemy[ENEMY_CX] + (int)(enemy[ENEMY_RADIUS] 
                  * (float)Math.cos(enemy[ENEMY_V] / d));
              enemy[ENEMY_Y] = enemy[ENEMY_CY] + (int)(enemy[ENEMY_RADIUS] 
                  * (float)Math.sin(enemy[ENEMY_V] / d));
              enemy[ENEMY_V]++;
            } else if (enemy[ENEMY_TYPE] == ENEMY_TYPE_SPIKES) {

                switch (enemy[ENEMY_Y]) {
                    case 0:
                        enemy[ENEMY_V] = 1;
                        break;
                    case FRAME_HEIGHT - 10:
                        enemy[ENEMY_V] = -1;
                        break;
                }
              enemy[ENEMY_Y] += enemy[ENEMY_V];
            } else if (enemy[ENEMY_TYPE] != ENEMY_TYPE_GREEN) {
              
              if (enemy[ENEMY_X] % 10 == 0) {                
                x = ((enemy[ENEMY_X] / 10) 
                    + enemy[ENEMY_V] + MAP_WIDTH) % MAP_WIDTH;
                y = enemy[ENEMY_Y] / 10;
                i = map[y][x];
                j = map[(y  
                    + (enemy[ENEMY_TYPE] == ENEMY_TYPE_FLIPPED_RED ? -1 : 1) 
                        + MAP_HEIGHT) % MAP_HEIGHT][x];
                if ((i != TILE_EMPTY && i != TILE_ANTIGRAVITY 
                        && i != TILE_APPEARING_BLOCK) 
                    || (j == TILE_EMPTY || j == TILE_ANTIGRAVITY 
                        || j == TILE_APPEARING_BLOCK)) {
                  enemy[ENEMY_V] = -enemy[ENEMY_V];
                }
              }
              enemy[ENEMY_X] = (enemy[ENEMY_X] + enemy[ENEMY_V] 
                  + FRAME_WIDTH) % FRAME_WIDTH;
            } else {
              
              if (enemy[ENEMY_Y] % 10 == 0) {
                x = enemy[ENEMY_X] / 10;
                y = ((enemy[ENEMY_Y] / 10) 
                    + enemy[ENEMY_V] + MAP_HEIGHT) % MAP_HEIGHT;
                z = map[(y + MAP_HEIGHT) % MAP_HEIGHT][x];
                if (z != TILE_EMPTY && z != TILE_ANTIGRAVITY 
                    && z != TILE_APPEARING_BLOCK) {
                  enemy[ENEMY_V] = -enemy[ENEMY_V];
                }
              }
              enemy[ENEMY_Y] = (enemy[ENEMY_Y] + enemy[ENEMY_V] 
                  + FRAME_HEIGHT) % FRAME_HEIGHT;
            }
          }
          
          
          x = ((playerX + FRAME_WIDTH) % FRAME_WIDTH) 
              - ((enemy[ENEMY_X] + FRAME_WIDTH) % FRAME_WIDTH);
          y = (((int)playerY + FRAME_HEIGHT) % FRAME_HEIGHT) 
              - ((enemy[ENEMY_Y] + FRAME_HEIGHT) % FRAME_HEIGHT);
          if (x <= 9 && x >= -9 && y <= 9 && y >= -9) { 
            
            resetDelay = 1;
            advanceLevel = 0;
          }
        }
        
        if (diamonds == 0) {
          
          resetDelay = ADVANCE_LEVEL_DELAY;
          advanceLevel = 1;
        }
        

      } while(nextFrameStartTime < System.nanoTime());

      
            
      
      
      
      g.setColor(BACKGROUND_COLOR);
      g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
      
      
      for(y = 0; y < MAP_HEIGHT; y++) {
        for(x = 0; x < MAP_WIDTH; x++) {
          i = map[y][x];
          if (i != TILE_EMPTY) {
            g.drawImage(tiles[i], 10 * x, 10 * y, null);
          }
        }
      }
      
      
      for(k = 0; k < enemies.size(); k++) {
        enemy = enemies.get(k);
        for(i = 0; i < 3; i++) {
          for(j = 0; j < 3; j++) {
            g.drawImage(tiles[TILE_RED_ENEMY_0 + ((counter >> 4) & 1)
                    + (enemy[ENEMY_TYPE] << 1)], 
                enemy[ENEMY_X] + (j - 1) * FRAME_WIDTH, 
                enemy[ENEMY_Y] + (i - 1) * FRAME_HEIGHT, null);
          }
        }  
      }
      
      
      for(i = 0; i < 3; i++) {
        for(j = 0; j < 3; j++) {
          g.drawImage(tiles[TILE_PLAYER_0 + ((arrowTime >> 3) & 1) 
                  + playerSpriteOffset], 
              playerX + (j - 1) * FRAME_WIDTH,
              (int)playerY + (i - 1) * FRAME_HEIGHT, null);  
        }
      }      
      
      
      if (g2 == null) {
        g2 = (Graphics2D)getGraphics();        
      } else {
        g2.drawImage(imageBuffer, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null);
      }

      
      while(nextFrameStartTime - System.nanoTime() > 0);
    }
  }
  
  @Override
  public boolean handleEvent(Event e) {
    
    final var KEY_LEFT = 1006;
    final var KEY_RIGHT = 1007;
    final var KEY_N = 110;
    final var KEY_P = 112;
    final var KEY_R = 114;

      for (var i : new int[]{KEY_LEFT, KEY_RIGHT, KEY_N, KEY_P, KEY_R}) {
      if (e.key == i) {
        return a[e.key]
                = e.id == 401 || e.id == 403;
      }
    }
      final var KEY_X = 120;
      return a[KEY_X]
            = e.id == 401 || e.id == 403;
  }  

  
  public static void main(String[] args) throws Throwable {
    var frame = new javax.swing.JFrame(
        "Dord");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    var applet = new a();
    applet.setPreferredSize(new java.awt.Dimension(800, 600));
    frame.add(applet, java.awt.BorderLayout.CENTER);
    frame.setResizable(false);    
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);    
    Thread.sleep(250);
    applet.start();
    applet.requestFocus();
  }
}
