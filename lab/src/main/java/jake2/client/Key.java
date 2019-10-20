/*
 * Key.java
 * Copyright (C) 2003
 * 
 * $Id: Key.java,v 1.12 2005-12-18 22:10:12 cawe Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.client;

import jake2.Defines;
import jake2.Globals;
import jake2.game.Cmd;
import jake2.qcommon.Cbuf;
import jake2.qcommon.Com;
import jake2.qcommon.Cvar;
import jake2.qcommon.xcommand_t;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Key
 */
public class Key extends Globals {
	
	
	
	public static final int K_TAB = 9;
	public static final int K_ENTER = 13;
	public static final int K_ESCAPE = 27;
	public static final int K_SPACE = 32;

	

	public static final int K_BACKSPACE = 127;
	public static final int K_UPARROW = 128;
	public static final int K_DOWNARROW = 129;
	public static final int K_LEFTARROW = 130;
	public static final int K_RIGHTARROW = 131;

	public static final int K_ALT = 132;
	public static final int K_CTRL = 133;
	public static final int K_SHIFT = 134;
	public static final int K_F1 = 135;
	public static final int K_F2 = 136;
	public static final int K_F3 = 137;
	public static final int K_F4 = 138;
	public static final int K_F5 = 139;
	public static final int K_F6 = 140;
	public static final int K_F7 = 141;
	public static final int K_F8 = 142;
	public static final int K_F9 = 143;
	public static final int K_F10 = 144;
	public static final int K_F11 = 145;
	public static final int K_F12 = 146;
	public static final int K_INS = 147;
	public static final int K_DEL = 148;
	public static final int K_PGDN = 149;
	public static final int K_PGUP = 150;
	public static final int K_HOME = 151;
	public static final int K_END = 152;

	public static final int K_KP_HOME = 160;
	public static final int K_KP_UPARROW = 161;
	public static final int K_KP_PGUP = 162;
	public static final int K_KP_LEFTARROW = 163;
	public static final int K_KP_5 = 164;
	public static final int K_KP_RIGHTARROW = 165;
	public static final int K_KP_END = 166;
	public static final int K_KP_DOWNARROW = 167;
	public static final int K_KP_PGDN = 168;
	public static final int K_KP_ENTER = 169;
	public static final int K_KP_INS = 170;
	public static final int K_KP_DEL = 171;
	public static final int K_KP_SLASH = 172;
	public static final int K_KP_MINUS = 173;
	public static final int K_KP_PLUS = 174;

	public static final int K_PAUSE = 255;

	
	
	
	public static final int K_MOUSE1 = 200;
	public static final int K_MOUSE2 = 201;
	public static final int K_MOUSE3 = 202;

	
	
	
	public static final int K_JOY1 = 203;
	public static final int K_JOY2 = 204;
	public static final int K_JOY3 = 205;
	public static final int K_JOY4 = 206;

	public static final int K_MWHEELDOWN = 239;
	public static final int K_MWHEELUP = 240;

	static int anykeydown;
	static int key_waiting;
	static int history_line;
	static boolean shift_down;
	static final int[] key_repeats = new int[256];
	
	static final boolean[] menubound = new boolean[256];
	static final boolean[] consolekeys = new boolean[256];

	static final String[] keynames = new String[256];

	static {
		keynames[K_TAB] = "TAB";
		keynames[K_ENTER] = "ENTER";
		keynames[K_ESCAPE] = "ESCAPE";
		keynames[K_SPACE] = "SPACE";
		keynames[K_BACKSPACE] = "BACKSPACE";
		keynames[K_UPARROW] = "UPARROW";
		keynames[K_DOWNARROW] = "DOWNARROW";
		keynames[K_LEFTARROW] = "LEFTARROW";
		keynames[K_RIGHTARROW] = "RIGHTARROW";
		keynames[K_ALT] = "ALT";
		keynames[K_CTRL] = "CTRL";
		keynames[K_SHIFT] = "SHIFT";

		keynames[K_F1] = "F1";
		keynames[K_F2] = "F2";
		keynames[K_F3] = "F3";
		keynames[K_F4] = "F4";
		keynames[K_F5] = "F5";
		keynames[K_F6] = "F6";
		keynames[K_F7] = "F7";
		keynames[K_F8] = "F8";
		keynames[K_F9] = "F9";
		keynames[K_F10] = "F10";
		keynames[K_F11] = "F11";
		keynames[K_F12] = "F12";

		keynames[K_INS] = "INS";
		keynames[K_DEL] = "DEL";
		keynames[K_PGDN] = "PGDN";
		keynames[K_PGUP] = "PGUP";
		keynames[K_HOME] = "HOME";
		keynames[K_END] = "END";

		keynames[K_MOUSE1] = "MOUSE1";
		keynames[K_MOUSE2] = "MOUSE2";
		keynames[K_MOUSE3] = "MOUSE3";

		
		
		
		

		keynames[K_KP_HOME] = "KP_HOME";
		keynames[K_KP_UPARROW] = "KP_UPARROW";
		keynames[K_KP_PGUP] = "KP_PGUP";
		keynames[K_KP_LEFTARROW] = "KP_LEFTARROW";
		keynames[K_KP_5] = "KP_5";
		keynames[K_KP_RIGHTARROW] = "KP_RIGHTARROW";
		keynames[K_KP_END] = "KP_END";
		keynames[K_KP_DOWNARROW] = "KP_DOWNARROW";
		keynames[K_KP_PGDN] = "KP_PGDN";
		keynames[K_KP_ENTER] = "KP_ENTER";
		keynames[K_KP_INS] = "KP_INS";
		keynames[K_KP_DEL] = "KP_DEL";
		keynames[K_KP_SLASH] = "KP_SLASH";

		keynames[K_KP_PLUS] = "KP_PLUS";
		keynames[K_KP_MINUS] = "KP_MINUS";

		keynames[K_MWHEELUP] = "MWHEELUP";
		keynames[K_MWHEELDOWN] = "MWHEELDOWN";

		keynames[K_PAUSE] = "PAUSE";
		keynames[';'] = "SEMICOLON"; 

		keynames[0] = "NULL";
	}

	/**
	 * 
	 */
	public static void Init() {
		for (int i = 0; i < 32; i++) {
			Globals.key_lines[i][0] = ']';
			Globals.key_lines[i][1] = 0;
		}
		Globals.key_linepos = 1;

		
		
		
		for (int i = 32; i < 128; i++)
			consolekeys[i] = true;
		consolekeys[K_ENTER] = true;
		consolekeys[K_KP_ENTER] = true;
		consolekeys[K_TAB] = true;
		consolekeys[K_LEFTARROW] = true;
		consolekeys[K_KP_LEFTARROW] = true;
		consolekeys[K_RIGHTARROW] = true;
		consolekeys[K_KP_RIGHTARROW] = true;
		consolekeys[K_UPARROW] = true;
		consolekeys[K_KP_UPARROW] = true;
		consolekeys[K_DOWNARROW] = true;
		consolekeys[K_KP_DOWNARROW] = true;
		consolekeys[K_BACKSPACE] = true;
		consolekeys[K_HOME] = true;
		consolekeys[K_KP_HOME] = true;
		consolekeys[K_END] = true;
		consolekeys[K_KP_END] = true;
		consolekeys[K_PGUP] = true;
		consolekeys[K_KP_PGUP] = true;
		consolekeys[K_PGDN] = true;
		consolekeys[K_KP_PGDN] = true;
		consolekeys[K_SHIFT] = true;
		consolekeys[K_INS] = true;
		consolekeys[K_KP_INS] = true;
		consolekeys[K_KP_DEL] = true;
		consolekeys[K_KP_SLASH] = true;
		consolekeys[K_KP_PLUS] = true;
		consolekeys[K_KP_MINUS] = true;
		consolekeys[K_KP_5] = true;

		consolekeys['`'] = false;
		consolekeys['~'] = false;



























		menubound[K_ESCAPE] = true;
		for (int i = 0; i < 12; i++)
			menubound[K_F1 + i] = true;

		
		
		
		Cmd.AddCommand("bind", Key.Bind_f);
		Cmd.AddCommand("unbind", Key.Unbind_f);
		Cmd.AddCommand("unbindall", Key.Unbindall_f);
		Cmd.AddCommand("bindlist", Key.Bindlist_f);
	}

	public static void ClearTyping() {
		Globals.key_lines[Globals.edit_line][1] = 0; 
		Globals.key_linepos = 1;
	}

	/**
	 * Called by the system between frames for both key up and key down events.
	 */
	public static void Event(int key, boolean down, int time) {


        if (key_waiting == -1) {
			if (down)
				key_waiting = key;
			return;
		}

		
		if (down) {
			key_repeats[key]++;
			if (key_repeats[key] > 1
				&& Globals.cls.key_dest == Defines.key_game
				&& !(Globals.cls.state == Defines.ca_disconnected))
				return; 

			if (key >= 200 && Globals.keybindings[key] == null)
				Com.Printf(Key.KeynumToString(key) + " is unbound, hit F4 to setAt.\n");
		}
		else {
			key_repeats[key] = 0;
		}

		if (key == K_SHIFT)
			shift_down = down;

		
		if (key == '`' || key == '~') {
			if (!down)
				return;

			Console.ToggleConsole_f.execute();
			return;
		}

		
		if (Globals.cl.attractloop && Globals.cls.key_dest != Defines.key_menu && !(key >= K_F1 && key <= K_F12))
			key = K_ESCAPE;

		
		if (key == K_ESCAPE) {
			if (!down)
				return;

			if (Globals.cl.frame.playerstate.stats[Defines.STAT_LAYOUTS] != 0 && Globals.cls.key_dest == Defines.key_game) {
				
				Cbuf.AddText("cmd putaway\n");
				return;
			}
			switch (Globals.cls.key_dest) {
				case Defines.key_message :
					Key.Message(K_ESCAPE);
					break;
				case Defines.key_menu :
					Menu.Keydown(K_ESCAPE);
					break;
				case Defines.key_game :
				case Defines.key_console :
					Menu.Menu_Main_f();
					break;
				default :
					Com.Error(Defines.ERR_FATAL, "Bad cls.key_dest");
			}
			return;
		}

		
		Globals.keydown[key] = down;
		if (down) {
			if (key_repeats[key] == 1)
				Key.anykeydown++;
		}
		else {
			Key.anykeydown--;
			if (Key.anykeydown < 0)
				Key.anykeydown = 0;
		}


        String cmd;
        String kb;
        if (!down) {
			kb = Globals.keybindings[key];
			if (kb != null && kb.length()>0 && kb.charAt(0) == '+') {
				cmd = '-' + kb.substring(1) + ' ' + key + ' ' + time + '\n';
				Cbuf.AddText(cmd);
			}







			return;
		}

		
		
		
		if ((Globals.cls.key_dest == Defines.key_menu && menubound[key])
			|| (Globals.cls.key_dest == Defines.key_console && !consolekeys[key])
			|| (Globals.cls.key_dest == Defines.key_game && (Globals.cls.state == Defines.ca_active || !consolekeys[key]))) {
			kb = Globals.keybindings[key];
			if (kb != null) {
				if (kb.length()>0 && kb.charAt(0) == '+') {
					
					cmd = kb + ' ' + key + ' ' + time + '\n';
					Cbuf.AddText(cmd);
				}
				else {
					Cbuf.AddText(kb + '\n');
				}
			}
			return;
		}

		if (!down)
			return; 




		switch (Globals.cls.key_dest) {
			case Defines.key_message :
				Key.Message(key);
				break;
			case Defines.key_menu :
				Menu.Keydown(key);
				break;

			case Defines.key_game :
			case Defines.key_console :
				Key.Console(key);
				break;
			default :
				Com.Error(Defines.ERR_FATAL, "Bad cls.key_dest");
		}
	}

	/**
	 * Returns a string (either a single ascii char, or a K_* name) for the 
	 * given keynum.
	 */
	public static String KeynumToString(int keynum) {
		if (keynum < 0 || keynum > 255)
			return "<KEY NOT FOUND>";
		if (keynum > 32 && keynum < 127)
			return Character.toString((char) keynum);

		if (keynames[keynum] != null)
			return keynames[keynum];

		return "<UNKNOWN KEYNUM>";
	}

	/**
	 * Returns a key number to be used to index keybindings[] by looking at
	 * the given string. Single ascii characters return themselves, while
	 * the K_* names are matched up.
	 */
	static int StringToKeynum(String str) {

		if (str == null)
			return -1;

		if (str.length() == 1)
			return str.charAt(0);

        int bound = keynames.length;
		for (int i = 0; i < bound; i++) {
			if (str.equalsIgnoreCase(keynames[i])) {
				return i;
			}
		}
		return -1;

	}

	public static void Message(int key) {

		if (key == K_ENTER || key == K_KP_ENTER) {
			if (Globals.chat_team)
				Cbuf.AddText("say_team \"");
			else
				Cbuf.AddText("say \"");

			Cbuf.AddText(Globals.chat_buffer);
			Cbuf.AddText("\"\n");

			Globals.cls.key_dest = Defines.key_game;
			Globals.chat_buffer = "";
			return;
		}

		if (key == K_ESCAPE) {
			Globals.cls.key_dest = Defines.key_game;
			Globals.chat_buffer = "";
			return;
		}

		if (key < 32 || key > 127)
			return; 

		if (key == K_BACKSPACE) {
			if (Globals.chat_buffer.length() > 2) {
				Globals.chat_buffer = Globals.chat_buffer.substring(0, Globals.chat_buffer.length() - 2);
			}
			else
				Globals.chat_buffer = "";
			return;
		}

		if (Globals.chat_buffer.length() > Defines.MAXCMDLINE)
			return; 

		Globals.chat_buffer += (char) key;
	}

	/**
	 * Interactive line editing and console scrollback.
	 */
	public static void Console(int key) {

		switch (key) {
			case K_KP_SLASH:
				key = '/';
				break;
			case K_KP_MINUS:
				key = '-';
				break;
			case K_KP_PLUS:
				key = '+';
				break;
			case K_KP_HOME:
				key = '7';
				break;
			case K_KP_UPARROW:
				key = '8';
				break;
			case K_KP_PGUP:
				key = '9';
				break;
			case K_KP_LEFTARROW:
				key = '4';
				break;
			case K_KP_5:
				key = '5';
				break;
			case K_KP_RIGHTARROW:
				key = '6';
				break;
			case K_KP_END:
				key = '1';
				break;
			case K_KP_DOWNARROW:
				key = '2';
				break;
			case K_KP_PGDN:
				key = '3';
				break;
			case K_KP_INS:
				key = '0';
				break;
			case K_KP_DEL:
				key = '.';
				break;
		}

        boolean finished = false;
		if (key == 'l') {
			if (Globals.keydown[K_CTRL]) {
				Cbuf.AddText("clear\n");
				finished = true;
			}
		}
		if (!finished) {
			if (key == K_ENTER || key == K_KP_ENTER) {

				if (Globals.key_lines[Globals.edit_line][1] == '\\' || Globals.key_lines[Globals.edit_line][1] == '/')
					Cbuf.AddText(
							new String(Globals.key_lines[Globals.edit_line], 2, Lib.strlen(Globals.key_lines[Globals.edit_line]) - 2));
				else
					Cbuf.AddText(
							new String(Globals.key_lines[Globals.edit_line], 1, Lib.strlen(Globals.key_lines[Globals.edit_line]) - 1));


				Cbuf.AddText("\n");

				Com.Printf(new String(Globals.key_lines[Globals.edit_line], 0, Lib.strlen(Globals.key_lines[Globals.edit_line])) + '\n');
				Globals.edit_line = (Globals.edit_line + 1) & 31;
				history_line = Globals.edit_line;

				Globals.key_lines[Globals.edit_line][0] = ']';
				Globals.key_linepos = 1;
				if (Globals.cls.state == Defines.ca_disconnected)
					SCR.UpdateScreen();
			} else if (key == K_TAB) {

				CompleteCommand();
			} else if ((key == K_BACKSPACE) || (key == K_LEFTARROW) || (key == K_KP_LEFTARROW) || ((key == 'h') && (Globals.keydown[K_CTRL]))) {
				if (Globals.key_linepos > 1)
					Globals.key_linepos--;
			} else if ((key == K_UPARROW) || (key == K_KP_UPARROW) || ((key == 'p') && Globals.keydown[K_CTRL])) {
				do {
					history_line = (history_line - 1) & 31;
				}
				while (history_line != Globals.edit_line && Globals.key_lines[history_line][1] == 0);
				if (history_line == Globals.edit_line)
					history_line = (Globals.edit_line + 1) & 31;

				System.arraycopy(Globals.key_lines[history_line], 0, Globals.key_lines[Globals.edit_line], 0, Globals.key_lines[Globals.edit_line].length);
				Globals.key_linepos = Lib.strlen(Globals.key_lines[Globals.edit_line]);
			} else if ((key == K_DOWNARROW) || (key == K_KP_DOWNARROW) || ((key == 'n') && Globals.keydown[K_CTRL])) {
				if (history_line != Globals.edit_line) {
					do {
						history_line = (history_line + 1) & 31;
					}
					while (history_line != Globals.edit_line && Globals.key_lines[history_line][1] == 0);
					if (history_line == Globals.edit_line) {
						Globals.key_lines[Globals.edit_line][0] = ']';
						Globals.key_linepos = 1;
					} else {

						System.arraycopy(Globals.key_lines[history_line], 0, Globals.key_lines[Globals.edit_line], 0, Globals.key_lines[Globals.edit_line].length);
						Globals.key_linepos = Lib.strlen(Globals.key_lines[Globals.edit_line]);
					}
				}
			} else if (key == K_PGUP || key == K_KP_PGUP) {
				Globals.con.display -= 2;
			} else if (key == K_PGDN || key == K_KP_PGDN) {
				Globals.con.display += 2;
				if (Globals.con.display > Globals.con.current)
					Globals.con.display = Globals.con.current;
			} else if (key == K_HOME || key == K_KP_HOME) {
				Globals.con.display = Globals.con.current - Globals.con.totallines + 10;
			} else if (key == K_END || key == K_KP_END) {
				Globals.con.display = Globals.con.current;
			} else if (key >= 32 && key <= 127) {
				if (Globals.key_linepos < Defines.MAXCMDLINE - 1) {
					Globals.key_lines[Globals.edit_line][Globals.key_linepos] = (byte) key;
					Globals.key_linepos++;
					Globals.key_lines[Globals.edit_line][Globals.key_linepos] = 0;
				}
			}

		}

	}

	private static void printCompletions(String type, List compl) {
		Com.Printf(type);
		for (int i = 0; i < compl.size(); i++) {
			Com.Printf(compl.get(i) + " ");
		}
		Com.Printf("\n");
	}
	
	static void CompleteCommand() {

        int start = 1;
		if (key_lines[edit_line][start] == '\\' ||  key_lines[edit_line][start] == '/')
			start++;

        int end = start;
		while (key_lines[edit_line][end] != 0) end++;

        String s = new String(key_lines[edit_line], start, end-start);

        List cmds = Cmd.CompleteCommand(s);
        List vars = Cvar.CompleteVariable(s);

        int c = cmds.size();
        int v = vars.size();
		
		if ((c + v) > 1) {
			if (c > 0) printCompletions("\nCommands:\n", cmds);
			if (v > 0) printCompletions("\nVariables:\n", vars);
			return;
		} else if (c == 1) {
			s = (String)cmds.get(0);
		} else if (v == 1) {
			s = (String)vars.get(0);
		} else return;
		
		key_lines[edit_line][1] = '/';
        byte[] bytes = Lib.stringToBytes(s);
		System.arraycopy(bytes, 0, key_lines[edit_line], 2, bytes.length);
		key_linepos = bytes.length + 2;
		key_lines[edit_line][key_linepos++] = ' ';
		key_lines[edit_line][key_linepos] = 0;

	}

	public static final xcommand_t Bind_f = new xcommand_t() {
		@Override
        public void execute() {
			Key_Bind_f();
		}
	};

	static void Key_Bind_f() {
        int c = Cmd.Argc();

		if (c < 2) {
			Com.Printf("bind <key> [command] : attach a command to a key\n");
			return;
		}
        int b = StringToKeynum(Cmd.Argv(1));
		if (b == -1) {
			Com.Printf('"' + Cmd.Argv(1) + "\" isn't a valid key\n");
			return;
		}

		if (c == 2) {
			if (Globals.keybindings[b] != null)
				Com.Printf('"' + Cmd.Argv(1) + "\" = \"" + Globals.keybindings[b] + "\"\n");
			else
				Com.Printf('"' + Cmd.Argv(1) + "\" is not bound\n");
			return;
		}


        String cmd = "";
		for (int i = 2; i < c; i++) {
			cmd += Cmd.Argv(i);
			if (i != (c - 1))
				cmd += " ";
		}

		SetBinding(b, cmd);
	}

	static void SetBinding(int keynum, String binding) {
		if (keynum == -1)
			return;

		
		Globals.keybindings[keynum] = null;

		Globals.keybindings[keynum] = binding;
	}

	static final xcommand_t Unbind_f = new xcommand_t() {
		@Override
        public void execute() {
			Key_Unbind_f();
		}
	};

	static void Key_Unbind_f() {

		if (Cmd.Argc() != 2) {
			Com.Printf("unbind <key> : remove commands from a key\n");
			return;
		}

        int b = Key.StringToKeynum(Cmd.Argv(1));
		if (b == -1) {
			Com.Printf('"' + Cmd.Argv(1) + "\" isn't a valid key\n");
			return;
		}

		Key.SetBinding(b, null);
	}

	static final xcommand_t Unbindall_f = new xcommand_t() {
		@Override
        public void execute() {
			Key_Unbindall_f();
		}
	};

	static void Key_Unbindall_f() {
		for (int i = 0; i < 256; i++)
			Key.SetBinding(i, null);
	}

	static final xcommand_t Bindlist_f = new xcommand_t() {
		@Override
        public void execute() {
			Key_Bindlist_f();
		}
	};

	static void Key_Bindlist_f() {
		for (int i = 0; i < 256; i++)
			if (Globals.keybindings[i] != null && Globals.keybindings[i].length() != 0)
				Com.Printf(Key.KeynumToString(i) + " \"" + Globals.keybindings[i] + "\"\n");
	}

	static void ClearStates() {

        Key.anykeydown = 0;

		for (int i = 0; i < 256; i++) {
			if (keydown[i] || key_repeats[i]!=0)
				Event(i, false, 0);
			keydown[i] = false;
			key_repeats[i] = 0;
		}
	}

	public static void WriteBindings(RandomAccessFile f) {
		for (int i = 0; i < 256; i++)
			if (keybindings[i] != null && keybindings[i].length() > 0)
				try {
					f.writeBytes("bind " + KeynumToString(i) + " \"" + keybindings[i] + "\"\n");
				} catch (IOException e) {}
	}

}
