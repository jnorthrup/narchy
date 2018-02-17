/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */
package org.boon.di;

import org.junit.jupiter.api.Test;

import static org.boon.Exceptions.die;

//@the("/coffeeApp")
public class CoffeeApp implements Runnable {

    @need
    CoffeeMaker coffeeMaker;

    @need
    Coffee coffee;

    @need
    Sugar sugar;

    @need
    Electricity ac;

    @need
    @the("cellphoneFood")
    Electricity dc;

    @need
    @the("cat")
    Food catFood;

    @need
    @the("american")
    Food americanFood;

    @need
    @the("soft")
    Food softFood;


    @the("rick's habit") @need
    Coffee rickCoffee;


    @the("black") @need
    Coffee blackCoffee;


    @the("hallwaySocket") @need
    java.util.function.Supplier<org.boon.di.Electricity> wallOutlet;

    //Todo this works but I need a real unit test.
    //    @Inject
    //    @Named( "this is not found" )
    //    @Required
    //    Coffee notFound;
    //    @In("more stuff not found")
    //    Coffee notFound2;
    boolean started = false;

    @start
    void init() {
        started = true;
    }

    @Override
    public void run() {
        coffeeMaker.brew();
    }

    @Test
    public void test() {
        CoffeeApp.main();
    }

    //singleton ish
    static Sugar staticSugar = new Sugar();

    //prototype
    static Electricity prorotypeElectricity = new Electricity();

    static {
        prorotypeElectricity.ACorDC = true;
    }

    static Thing m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12;

    static void createModules() {
        m1 = My.classes(CoffeeApp.class, CoffeeMaker.class, FoodImpl.class);
        m2 = My.thing(new DripCoffee());
        m3 = My.thing(new PumpSystem());
        m4 = My.supply(Coffee.class, Coffee::new);
        m5 = My.object(staticSugar);
        m6 = My.prototype(prorotypeElectricity);
        m7 = My.supply(
                Supply.of(Electricity.class),
                Supply.of("hallwaySocket", Electricity.class),
                Supply.of("red", new Electricity()),
                Supply.of("brown", Electricity.class, () -> {
                    Electricity ece = new Electricity();
                    ece.tag = "m7";
                    return ece;
                }));
        m0 = My.supply("cellphoneFood", () -> {
            Electricity ece = new Electricity();
            ece.tag = "m7";
            return ece;
        });
        m8 = My.clazz(CatFood.class);
        m9 = My.object(new SoylentGreen());
        m10 = My.supply(Supply.of("soft", new Noodles())); //TODO add shortcut method in My
        m11 = My.supply(Supply.of("rick's habit", Coffee.class)); //TODO add shortcut method in My
    }

    public static void main(String... args) {
        createModules();
        That stuff = My.of(m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m0);
        Heater heater = stuff.a(Heater.class);
        boolean ok = heater instanceof ElectricHeater || die();
        CoffeeApp coffeeApp = stuff.a(CoffeeApp.class);
        coffeeApp.run();
        validateApp(coffeeApp);
        createModules();
        stuff = My.of(m0, m8, m9, m10, m11, m1, m2, m3, m4, m5, m6, m7);
        coffeeApp = stuff.a(CoffeeApp.class);
        validateApp(coffeeApp);
        Electricity ece = stuff.a(Electricity.class, "cellphoneFood");
        ok = ece != null || die();
        ok = stuff.has(Coffee.class) || die();
        ok = stuff.has("black") || die();
        ok = stuff.a("black") != null || die();
        ok = stuff.a("electricHeater") != null || die();
        ok = stuff.a("foodImpl") != null || die();
        stuff.remove(m0);
        stuff.add(m11);
        stuff.addFirst(m0);
        coffeeApp = stuff.a(CoffeeApp.class);
        validateApp(coffeeApp);
        stuff.a(SoylentGreen.class);
        stuff.a(SoylentGreen.class);
        stuff.a(SoylentGreen.class);
        stuff.a(SoylentGreen.class);
        stuff.a(SoylentGreen.class, "american");
    }

    private static void validateApp(CoffeeApp coffeeApp) {
        boolean ok;
        ok = coffeeApp.started || die();
        ok = coffeeApp.coffee != null || die();
        ok = coffeeApp.sugar == staticSugar || die();
        ok = coffeeApp.ac != prorotypeElectricity || die();
        ok = coffeeApp.ac.ACorDC || die();
        ok = coffeeApp.dc != null || die();
        ok = coffeeApp.catFood != null || die();
        ok = coffeeApp.catFood instanceof CatFood || die(coffeeApp.catFood.toString());
        ok = coffeeApp.americanFood != null || die();
        ok = coffeeApp.americanFood instanceof SoylentGreen || die(coffeeApp.americanFood.toString());
        ok = coffeeApp.softFood != null || die();
        ok = coffeeApp.softFood instanceof Noodles || die(coffeeApp.softFood.toString());
        //
        //        ok = coffeeApp.rickDrinks != null || die();
        //
        //        ok = ( coffeeApp.rickDrinks instanceof Coffee ) || die( coffeeApp.rickDrinks.toString() );
        //
        //
        //
        //        ok = coffeeApp.rickDrinks2 != null || die();
        //
        //        ok = ( coffeeApp.rickDrinks2 instanceof Coffee ) || die( coffeeApp.rickDrinks.toString() );
        ok = coffeeApp.rickCoffee != null || die();
        ok = (coffeeApp.rickCoffee instanceof Coffee) || die(coffeeApp.rickCoffee.toString());
        ok = coffeeApp.blackCoffee != null || die();
        ok = (coffeeApp.blackCoffee instanceof Coffee) || die(coffeeApp.blackCoffee.toString());
        ok = coffeeApp.wallOutlet != null || die();
        ok = (coffeeApp.wallOutlet.get() instanceof Electricity) || die(coffeeApp.wallOutlet.get().toString());
    }
}
