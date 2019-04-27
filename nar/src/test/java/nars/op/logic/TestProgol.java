package nars.op.logic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

/**
 * http:
 * http:
 */
class TestProgol {
    @Test
    void testAnimals() throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        /*
        % Mode declarations
        :- modeh(1,class(+animal,#class))?
        :- modeb(1,has_milk(+animal))?
        :- modeb(1,has_gills(+animal))?
        :- modeb(1,has_covering(+animal,#covering))?
        :- modeb(1,has_legs(+animal,#nat))?
        :- modeb(1,homeothermic(+animal))?
        :- modeb(1,has_eggs(+animal))?
        :- modeb(1,not has_milk(+animal))?
        :- modeb(1,not has_gills(+animal))?
        :- modeb(*,habitat(+animal,#habitat))?
        :- modeh(1,false)?
        :- modeb(1,class(+animal,#class))?
         */

        
        n.input(
        "animal(dog).  animal(dolphin).  animal(platypus).  animal(bat).\n" +
            "animal(trout).  animal(herring).  animal(shark). animal(eel).\n" +
            "animal(lizard).  animal(crocodile).  animal(t_rex).  animal(turtle).\n" +
            "animal(snake).  animal(eagle).  animal(ostrich).  animal(penguin).\n" +
            "species(mammal).  species(fish).  species(reptile).  species(bird).\n" +
                
            "covering(hair).  covering(none).  covering(scales).  covering(feathers).\n" +
            "habitat(land).  habitat(water).  habitat(air).  habitat(caves).");

        
        n.input(
                "class(dog,mammal).\n" +
                    "class(dolphin,mammal).\n" +
                    "class(platypus,mammal).\n" +
                    "class(bat,mammal).\n" +
                    "class(trout,fish).\n" +
                    "class(herring,fish).\n" +
                    "class(shark,fish).\n" +
                    "class(eel,fish).\n" +
                    "class(lizard,reptile).\n" +
                    "class(crocodile,reptile).\n" +
                    "class(t_rex,reptile).\n" +
                    "class(snake,reptile).\n" +
                    "class(turtle,reptile).\n" +
                    "class(eagle,bird).\n" +
                    "class(ostrich,bird).\n" +
                    "class(penguin,bird).");

        
        n.input(
            "--(class(#X,mammal) && class(#X,fish)).\n" +
                "--(class(#X,mammal) && class(#X,reptile)).\n" +
                "--(class(#X,mammal) && class(#X,bird)).\n" +
                "--(class(#X,fish) && class(#X,reptile)).\n" +
                "--(class(#X,fish) && class(#X,bird)).\n" +
                "--(class(#X,reptile) && class(#X,bird)).\n" +
                "--class(eagle,reptile).\n" +
                "--class(trout,mammal).\n" +
                "--class(herring,mammal).\n" +
                "--class(shark,mammal).\n" +
                "--class(lizard,mammal).\n" +
                "--class(crocodile,mammal).\n" +
                "--class(t_rex,mammal).\n" +
                "--class(turtle,mammal).\n" +
                "--class(eagle,mammal).\n" +
                "--class(ostrich,mammal).\n" +
                "--class(penguin,mammal).\n" +
                "--class(dog,fish).\n" +
                "--class(dolphin,fish).\n" +
                "--class(platypus,fish).\n" +
                "--class(bat,fish).\n" +
                "--class(lizard,fish).\n" +
                "--class(crocodile,fish).\n" +
                "--class(t_rex,fish).\n" +
                "--class(turtle,fish).\n" +
                "--class(eagle,fish).\n" +
                "--class(ostrich,fish).\n" +
                "--class(penguin,fish).\n" +
                "--class(dog,reptile).\n" +
                "--class(dolphin,reptile).\n" +
                "--class(platypus,reptile).\n" +
                "--class(bat,reptile).\n" +
                "--class(trout,reptile).\n" +
                "--class(herring,reptile).\n" +
                "--class(shark,reptile).\n" +
                "--class(eagle,reptile).\n" +
                "--class(ostrich,reptile).\n" +
                "--class(penguin,reptile).\n" +
                "--class(dog,bird).\n" +
                "--class(dolphin,bird).\n" +
                "--class(platypus,bird).\n" +
                "--class(bat,bird).\n" +
                "--class(trout,bird).\n" +
                "--class(herring,bird).\n" +
                "--class(shark,bird).\n" +
                "--class(lizard,bird).\n" +
                "--class(crocodile,bird).\n" +
                "--class(t_rex,bird).\n" +
                "--class(turtle,bird).");

        
        n.input(
            "has_covering(dog,hair).\n" +
                "has_covering(dolphin,none).\n" +
                "has_covering(platypus,hair).\n" +
                "has_covering(bat,hair).\n" +
                "has_covering(trout,scales).\n" +
                "has_covering(herring,scales).\n" +
                "has_covering(shark,none).\n" +
                "has_covering(eel,none).\n" +
                "has_covering(lizard,scales).\n" +
                "has_covering(crocodile,scales).\n" +
                "has_covering(t_rex,scales).\n" +
                "has_covering(snake,scales).\n" +
                "has_covering(turtle,scales).\n" +
                "has_covering(eagle,feathers).\n" +
                "has_covering(ostrich,feathers).\n" +
                "has_covering(penguin,feathers).\n" +
                "has_legs(dog,4).\n" +
                "has_legs(dolphin,0).\n" +
                "has_legs(platypus,2).\n" +
                "has_legs(bat,2).\n" +
                "has_legs(trout,0).\n" +
                "has_legs(herring,0).\n" +
                "has_legs(shark,0).\n" +
                "has_legs(eel,0).\n" +
                "has_legs(lizard,4).\n" +
                "has_legs(crocodile,4).\n" +
                "has_legs(t_rex,4).\n" +
                "has_legs(snake,0).\n" +
                "has_legs(turtle,4).\n" +
                "has_legs(eagle,2).\n" +
                "has_legs(ostrich,2).\n" +
                "has_legs(penguin,2).\n" +
                "has_milk(dog).\n" +
                "has_milk(dolphin).\n" +
                "has_milk(bat).\n" +
                "has_milk(platypus).\n" +
                "homeothermic(dog).\n" +
                "homeothermic(dolphin).\n" +
                "homeothermic(platypus).\n" +
                "homeothermic(bat).\n" +
                "homeothermic(eagle).\n" +
                "homeothermic(ostrich).\n" +
                "homeothermic(penguin).\n" +
                "habitat(dog,land).\n" +
                "habitat(dolphin,water).\n" +
                "habitat(platypus,water).\n" +
                "habitat(bat,air).\n" +
                "habitat(bat,caves).\n" +
                "habitat(trout,water).\n" +
                "habitat(herring,water).\n" +
                "habitat(shark,water).\n" +
                "habitat(eel,water).\n" +
                "habitat(lizard,land).\n" +
                "habitat(crocodile,water).\n" +
                "habitat(crocodile,land).\n" +
                "habitat(t_rex,land).\n" +
                "habitat(snake,land).\n" +
                "habitat(turtle,water).\n" +
                "habitat(eagle,air).\n" +
                "habitat(eagle,land).\n" +
                "habitat(ostrich,land).\n" +
                "habitat(penguin,water).\n" +
                "has_eggs(platypus).\n" +
                "has_eggs(trout).\n" +
                "has_eggs(herring).\n" +
                "has_eggs(shark).\n" +
                "has_eggs(eel).\n" +
                "has_eggs(lizard).\n" +
                "has_eggs(crocodile).\n" +
                "has_eggs(t_rex).\n" +
                "has_eggs(snake).\n" +
                "has_eggs(turtle).\n" +
                "has_eggs(eagle).\n" +
                "has_eggs(ostrich).\n" +
                "has_eggs(penguin).\n" +
                "has_gills(trout).\n" +
                "has_gills(herring).\n" +
                "has_gills(shark).\n" +
                "has_gills(eel).\n" +
                "animal(cat). animal(dragon).\n" +
                "animal(girl).\n" +
                "animal(boy).\n" +
                "has_milk(cat).\n" +
                "homeothermic(cat).\n");

        n.freqResolution.set(0.25f);
        n.stats(System.out);
        
        n.run(100);
        n.stats(System.out);

    }
}
