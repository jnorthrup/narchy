package befunge;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

/**
 * Created by didrik on 30.12.2014.
 */
public enum Befunge {
    ;

    public static void main(String[] args) throws IOException {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Board board = new Board();
        for(int y = 0; y < 25; y++){
            char[] line = stdin.readLine().toCharArray();
            if (line.length == 0) break;
            for(int x = 0; x < 80 && x < line.length; x++){
                board.put(y, x, line[x]);
            }
        }

        Pointer p = new Pointer(board);
        while(p.step());
    }

    /**
     * Created by didrik on 30.12.2014.
     */
    static class Pointer {
        private int x, y, dx, dy;
        private final Board board;


        private final @NotNull Map<Character, Runnable> map;
        private final @NotNull BefungeStack stack;

        public Pointer(Board board){
            x = y = 0;
            dx = 1;
            dy = 0;
            stack = new BefungeStack();
            this.board = board;
            map = new HashMap();
            map.put('+', () -> {
                long temp = stack.pop();
                temp += stack.pop();
                stack.push(temp);
            });

            map.put('-', () -> {
                long temp = -stack.pop();
                temp += stack.pop();
                stack.push(temp);
            });

            map.put('*', () -> {
                long temp = stack.pop();
                temp *= stack.pop();
                stack.push(temp);
            });

            map.put('/', () -> {
                long t1 = stack.pop();
                long t2 = stack.pop();
                stack.push(t2/t1);
            });

            map.put('%', () -> {
                long t1 = stack.pop();
                long t2 = stack.pop();
                stack.push(t2%t1);
            });

            map.put('!', () -> stack.push((long) (stack.pop() == 0 ? 1 : 0)));

            map.put('`', () -> stack.push((long) (stack.pop() <= stack.pop() ? 1 : 0)));

            map.put('<', this::left);

            map.put('>', this::right);

            map.put('v', this::down);

            map.put('^', this::up);

            map.put('?', () -> {
                int[] xarray = {-1, 1, 0, 0};
                int[] yarray = {0, 0, -1, 1};
                Random r = new Random();
                int t = r.nextInt(3);
                dx = xarray[t];
                dy = yarray[t];
            });

            map.put('_', () -> {
                if(stack.pop() == 0) right();
                else left();
            });

            map.put('|', () -> {
                if(stack.pop() == 0) down();
                else up();
            });

            map.put('"', () -> {
                move();
                char c = this.board.get(y, x);
                while(c != '"'){
                    stack.push((long) c);
                    move();
                    c = this.board.get(y, x);
                }
            });

            map.put(':', () -> stack.push(stack.peek()));

            map.put('\\', () -> {
                long t1 = stack.pop();
                long t2 = stack.pop();
                stack.push(t2);
                stack.push(t1);
            });

            map.put('$', stack::pop);

            map.put('.', () -> System.out.print(stack.pop()));

            map.put(',', () -> System.out.print((char) stack.pop().shortValue()));

            map.put('#', this::move);

            map.put('p', () -> {
                int y1 = stack.pop().intValue();
                int x1 = stack.pop().intValue();
                char v = (char)stack.pop().shortValue();
                this.board.put(y1, x1, v);
            });

            map.put('g', () -> {
                int y1 = stack.pop().intValue();
                int x1 = stack.pop().intValue();
                char c = this.board.get(y1, x1);
                stack.push((long) c);
            });

            map.put(' ', () -> {});
        }

        private void move(){
            char WIDTH = 80;
            x = (x+dx) % WIDTH;
            char HEIGHT = 25;
            y = (y+dy) % HEIGHT;
        }

        boolean step(){
            char c = board.get(y, x);
            if (c == '@') return false;
            if (Character.isDigit(c)) stack.push((long) c-'0');
            else map.get(c).run();
            move();
            return true;
        }

        private void right(){
            dx = 1;
            dy = 0;
        }

        private void left(){
            dx = -1;
            dy = 0;
        }

        private void down(){
            dx = 0;
            dy = 1;
        }

        private void up(){
            dx = 0;
            dy = -1;
        }

    }

    /**
     * Created by didrik on 30.12.2014.
     */
    public static class Board {

        private final @NotNull char[][] board;

        Board() {
            board = new char[25][80];
        }

        char get(int y, int x) {
            return board[y][x];
        }

        void put(int y, int x, char c) {
            board[y][x] = c;
        }
    }

    /**
     * Created by didrik on 30.12.2014.
     */
    public static class BefungeStack {

        Stack<Long> stack;

        public BefungeStack(){
            stack = new Stack();
        }

        void push(Long l){
            stack.push(l);
        }

        @NotNull
        Long pop(){
            return stack.isEmpty() ? 0L : stack.pop();
        }

        @NotNull
        Long peek(){
            return stack.isEmpty() ? 0L : stack.peek();
        }
    }
}
