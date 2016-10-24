"use strict";


/*var db = new loki('default', {
    //https://github.com/techfort/LokiJS/wiki/LokiJS-persistence-and-adapters
    adapter: new LokiIndexedAdapter('narchy'),
    autosave: true,
    autosaveInterval: 10000
} );(
*/


// /** client-side DB interface */
// function DB() {
//     const db = new loki('default', {});
//     return db;
// }
//
// function TaskDB(db, terminal) {
//
//
//     const tasks = db.addCollection('tasks', {
//         indices: ['occ', 'term', 'pri', 'conf', 'punc']
//     });
//
//     function arrayToDoc(a) {
//         return {
//             // String.valueOf(t.punc()),
//             punc: a[0],
//
//             // t.term().toString(),
//             term: a[1],
//
//             // truth!=null ? t.freq() : 0,
//             freq: a[2],
//             // truth!=null ? t.conf() : 0,
//             conf: a[3],
//
//             // occ!=ETERNAL ? occ : ":",
//             occ: a[4],
//
//             // Math.round(t.pri()*1000),
//             pri: a[5],
//
//             // Math.round(t.dur()*1000),
//             dur: a[6],
//
//             // Math.round(t.qua()*1000),
//             qua: a[7],
//
//             // t.lastLogged()
//             log: a[8]
//
//         };
//     }
//     /** inserts a task from the raw array format provided by a server */
//     tasks.addTaskArray = function(taskArrays/* array */) {
//
//         if (!Array.isArray(taskArrays))
//             taskArrays = [taskArrays];
//
//         _.each(taskArrays, function(i) { tasks.insert(arrayToDoc(i)); });
//
//
//     };
//
//     terminal.on('message', tasks.addTaskArray);
//
//     return tasks;
// }

const maxLabelLen = 16;
function labelize(l) {
    return l.length > maxLabelLen ? l.substr(0, maxLabelLen) + '..' : l;
}

function SocketNARGraph(path) {
    const sg = SocketSpaceGraph(path, function (x) {
            return x[0];
        },
        function (id, x, newNodes, newEdges) {

            const pri = x[1] / 1000.0;
            const qua = x[3] / 1000.0;

            const belief = x[5] ? [x[5][0] / 100.0, x[5][1] / 100.0] : [0.5, 0];
            const desire = x[6] ? [x[6][0] / 100.0, x[6][1] / 100.0] : [0.5, 0];

            newNodes.push({
                id: id,
                label: labelize(id),
                pri: pri,
                qua: qua,

                belief: 2.0 * (belief[0] - 0.5) * belief[1],
                desire: 2.0 * (desire[0] - 0.5) * desire[1]
            });

            const termlinks = x[4];
            if (termlinks) {
                for (let e of termlinks) {
                    /*if (!(e = e.seq))
                     return;*/
                    if (!e)
                        return;

                    const target = e[0];

                    const tlpri = e[1] / 1000.0;
                    const tldur = e[2] / 1000.0;
                    const tlqua = e[3] / 1000.0;

                    const tlPrefix = 'tl_' + id;
                    newEdges.push({
                        id: tlPrefix + '_' + target,
                        source: id, target: target,
                        pri: tlpri,
                        dur: tldur,
                        qua: tlqua
                    });
                }
            }



        }
    );

    // const layoutUpdatePeriodMS = 50;
    //
    // const currentLayout = sg.currentLayout = sg.spacegraph.makeLayout({
    //     name: 'cose',
    //     // Called on `layoutready`
    //     ready: function(){},
    //
    //     // Called on `layoutstop`
    //     stop: function(){},
    //
    //     // Whether to animate while running the layout
    //     animate: true,
    //
    //     // The layout animates only after this many milliseconds
    //     // (prevents flashing on fast runs)
    //     animationThreshold: 550,
    //
    //     // Number of iterations between consecutive screen positions update
    //     // (0 -> only updated on the end)
    //     refresh: 2,
    //
    //     // Whether to fit the network view after when done
    //     fit: true,
    //
    //     // Padding on fit
    //     padding: 30,
    //
    //     // Constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
    //     boundingBox: undefined,
    //
    //     // Randomize the initial positions of the nodes (true) or use existing positions (false)
    //     randomize: true,
    //
    //     // Extra spacing between components in non-compound graphs
    //     componentSpacing: 100,
    //
    //     // Node repulsion (non overlapping) multiplier
    //     nodeRepulsion: function( node ){ return 400000; },
    //
    //     // Node repulsion (overlapping) multiplier
    //     nodeOverlap: 10,
    //
    //     // Ideal edge (non nested) length
    //     idealEdgeLength: function( edge ){ return 10; },
    //
    //     // Divisor to compute edge forces
    //     edgeElasticity: function( edge ){ return 100; },
    //
    //     // Nesting factor (multiplier) to compute ideal edge length for nested edges
    //     nestingFactor: 5,
    //
    //     // Gravity force (constant)
    //     gravity: 80,
    //
    //     // Maximum number of iterations to perform
    //     numIter: 1000,
    //
    //     // Initial temperature (maximum node displacement)
    //     initialTemp: 200,
    //
    //     // Cooling factor (how the temperature is reduced between consecutive iterations
    //     coolingFactor: 0.95,
    //
    //     // Lower temperature threshold (below this point the layout will end)
    //     minTemp: 1.0,
    //
    //     // Whether to use threading to speed up the layout
    //     useMultitasking: true
    // });
    //
    // currentLayout.run();
    //
    // setInterval(()=>{
    //     currentLayout.stop();
    // }, layoutUpdatePeriodMS);

    // const currentLayout = sg.currentLayout = sg.spacegraph.makeLayout({
    //     /* https://github.com/cytoscape/cytoscape.js-spread */
    //     name: 'spread',
    //     minDist: 250,
    //     //padding: 100,
    //
    //     speed: 0.06,
    //     animate: false,
    //     randomize: false, // uses random initial node positions on true
    //     fit: false,
    //     maxFruchtermanReingoldIterations: 1, // Maximum number of initial force-directed iterations
    //     maxExpandIterations: 2, // Maximum number of expanding iterations
    //
    //     ready: function () {
    //         //console.log('starting cola', Date.now());
    //     },
    //     stop: function () {
    //         //console.log('stop cola', Date.now());
    //     }
    // });



    // const layout = function () {
    //     const currentLayout = sg.currentLayout;
    //     if (currentLayout) {
    //         currentLayout.stop();
    //         if (currentLayout.run) {
    //             currentLayout.run();
    //             setTimeout(layout, layoutUpdatePeriodMS); //self-trigger
    //         }
    //     }
    //
    //     /* https://github.com/cytoscape/cytoscape.js-cola#api */
    //
    //
    //     // if (currentLayout) {
    //     //     currentLayout.stop();
    //     // } else {
    //     //     // currentLayout = sg.makeLayout({
    //     //     //     name: 'cola',
    //     //     //     animate: true,
    //     //     //     fit: false,
    //     //     //     randomize: false,
    //     //     //     maxSimulationTime: 700, // max length in ms to run the layout
    //     //     //     speed: 1,
    //     //     //     refresh: 2,
    //     //     //     //infinite: true,
    //     //     //     nodeSpacing: function (node) {
    //     //     //         return 70;
    //     //     //     }, // extra spacing around nodes
    //     //     //
    //     //     //     ready: function () {
    //     //     //         //console.log('starting cola', Date.now());
    //     //     //     },
    //     //     //     stop: function () {
    //     //     //         //console.log('stop cola', Date.now());
    //     //     //     }
    //     //     // });
    //     //
    //     //
    //     // }
    //     //
    //     // currentLayout.run();
    //
    //
    //     sg.spacegraph.layout({ name: 'cose',
    //         animate: true,
    //         fit: false,
    //         refresh: 1,
    //         //animationThreshold: 1,
    //         iterations: 5000,
    //         initialTemp: 100,
    //         //coolingfactor: 0.98,
    //         ready: function() {
    //             //console.log('starting cose', Date.now());
    //         },
    //         stop: function() {
    //             //console.log('stop cose', Date.now());
    //         }
    //     });



    function d(x, key) {
        return x._private.data[key];// || 0;
    }

    const sizeFunc = function (x) {
        const p1 = 1 + d(x, 'pri');// * d(x, 'belief');
        return parseInt(24 + 48 * (p1 * p1));
    };


    // const priToOpacity = function (x) {
    //     const pri = d(x, 'pri');
    //     return (25 + parseInt(pri * 75)) / 100.0;
    // };
    sg.spacegraph.style().selector('node')
        .style('shape', 'hexagon')
        .style('width', sizeFunc)
        .style('height', sizeFunc)
        .style('background-color', function(x) {
            const belief = 0.25 + 0.75 * d(x, 'belief');
            const aBelief = 0.25 + 0.75 * Math.abs(belief);
            const pri = 0.25 + 0.75 * d(x, 'pri');


            const priColor = parseInt(pri * 255);
            const beliefColor = parseInt(aBelief * 255);
            const quaColor = parseInt(0.5 * 255);
                //const qua = d(x, 'qua');
                //const quaColor = parseInt((0.5 + 0.5 * qua) * 128);

            if (belief >= 0.05) {
                return "rgb(" + priColor + "," + beliefColor + "," + quaColor + ")";
            } else if (belief <= -0.05) {
                return "rgb(" + beliefColor + "," + priColor + "," + quaColor + ")";
            } else {
                return "rgb(" + priColor + "," + priColor + "," + quaColor + ")";
            }

        })
        //.style('background-opacity', priToOpacity)
        .style('background-opacity', 1.0)
        ;

    sg.spacegraph.style().selector('edge')
        .style('width', function(x) {
            return parseInt(2 + 5 * d(x, 'pri'));
        })
        .style('mid-target-arrow-shape', 'triangle')
        .style('opacity',
            // priToOpacity)
            1.0)
        .style('curve-style', function(x) {
            const pri = d(x, 'pri');
            if (pri < 0.25) return 'haystack';
            return 'segments';
        })
        .style('line-color', function(x) {
            return "rgb(" +
                parseInt((0.5 + 0.25 * d(x, 'pri')) * 255) + "," +
                parseInt((0.5 + 0.25 * d(x, 'dur')) * 255) + "," +
                parseInt((0.5 + 0.25 * d(x, 'qua')) * 255) + ")";
        });

    const coseDefault = {
        name: 'cose',

        // Called on `layoutready`
        ready: function () {
        },

        // Called on `layoutstop`
        stop: function () {
        },

        // Whether to animate while running the layout
        animate: false,

        // The layout animates only after this many milliseconds
        // (prevents flashing on fast runs)
        animationThreshold: 10,

        // Number of iterations between consecutive screen positions update
        // (0 -> only updated on the end)
        refresh: 0,

        // Whether to fit the network view after when done
        fit: false,

        // Padding on fit
        padding: 30,

        // Constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
        boundingBox: {x1: -3000, y1: -1000, x2: 3000, y2: 1000},

        // Randomize the initial positions of the nodes (true) or use existing positions (false)
        randomize: false,

        // Extra spacing between components in non-compound graphs
        componentSpacing: 10,

        // Node repulsion (non overlapping) multiplier
        nodeRepulsion: function (node) {
            return 40000;
        },

        // Node repulsion (overlapping) multiplier
        nodeOverlap: 10,

        // Ideal edge (non nested) length
        idealEdgeLength: function (edge) {
            return 2;
        },

        // Divisor to compute edge forces
        edgeElasticity: function (edge) {
            return 100;
        },

        // Nesting factor (multiplier) to compute ideal edge length for nested edges
        nestingFactor: 3,

        // Gravity force (constant)
        gravity: 80,

        // Maximum number of iterations to perform
        numIter: 1,

        // Initial temperature (maximum node displacement)
        initialTemp: 10,

        // Cooling factor (how the temperature is reduced between consecutive iterations
        coolingFactor: 0.98,

        // Lower temperature threshold (below this point the layout will end)
        minTemp: 1.0,

        // Whether to use threading to speed up the layout
        useMultitasking: true
    };
    let bfWorking = {
        name: 'breadthfirst',
        fit: false,
        avoidOverlap: true,
        maximalAdjustments: 2, // how many times to try to position the nodes in a maximal way (i.e. no backtracking)
        animate: true, // whether to transition the node positions
        animationDuration: 500, // duration of animation in ms if enabled
        animationEasing: undefined // easing of animation if enabled
    };

    let coseTweak = {
        name: 'cose',

        // Called on `layoutready`
        ready: function () {
            console.log('COSE layout ready');
        },

        // Called on `layoutstop`
        stop: function () {
        },

        // Whether to animate while running the layout
        animate: true,

        // The layout animates only after this many milliseconds
        // (prevents flashing on fast runs)
        animationThreshold: 500,

        // Number of iterations between consecutive screen positions update
        // (0 -> only updated on the end)
        refresh: 1,

        // Whether to fit the network view after when done
        fit: true,

        // Padding on fit
        padding: 30,

        // Constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
        boundingBox: undefined,

        // Randomize the initial positions of the nodes (true) or use existing positions (false)
        randomize: true,

        // Extra spacing between components in non-compound graphs
        componentSpacing: 100,

        // Node repulsion (non overlapping) multiplier
        nodeRepulsion: function (node) {
            return 400000;
        },

        // Node repulsion (overlapping) multiplier
        nodeOverlap: 10,

        // Ideal edge (non nested) length
        idealEdgeLength: function (edge) {
            return 10;
        },

        // Divisor to compute edge forces
        edgeElasticity: function (edge) {
            return 100;
        },

        // Nesting factor (multiplier) to compute ideal edge length for nested edges
        nestingFactor: 5,

        // Gravity force (constant)
        gravity: 80,

        // Maximum number of iterations to perform
        numIter: 1,

        // Initial temperature (maximum node displacement)
        initialTemp: 10,

        // Cooling factor (how the temperature is reduced between consecutive iterations
        coolingFactor: 1.0,

        // Lower temperature threshold (below this point the layout will end)
        minTemp: 0,

        // Whether to use threading to speed up the layout
        useMultitasking: true
    };

    setInterval(() => {
        // const layoutUpdatePeriodMS = 70;
        // setTimeout(layout, layoutUpdatePeriodMS);
        sg.spacegraph.layout(
            //bfWorking
            //coseTweak
            coseDefault
        );
    }, 100);


    return sg;
}


function NARTerminal() {

    const ws = NARSocket('terminal');

    const e = new EventEmitter();

    ws.onopen = function() {
        e.emit('connect', this);
    };

    ws.onmessage = function(m) {

        let d = m.data;

        try {

            e.emit('message', [
                (typeof d === "string") ?
                    JSON.parse(d) :
                    decodeBiNARy(e, d)
            ] );

        } catch (e) {
            console.error(e, d);
        }

    };

    ws.onclose = function() {
        e.emit('disconnect', this);
    };

    e.socket = ws;
    e.close = ws.close;
    e.send = function(x) {
        ws.send(x);
    };

    return e;
}

if (!("TextEncoder" in window))
    alert("Sorry, this browser does not support TextEncoder...");

function decodeBiNARy(e, m) {

    const d = new DataView(m);

    //assume 'd' is a NAR serialized Task
    let j = 0;
    let punct = d.getUint8(j++);
    //TODO use charCodePoint
    switch (punct) {

        case 46:
            punct = '.';
            break;
        case 33:
            punct = '!';
            break;
        case 63:
            punct = '?';
            break;
        case 64:
            punct = '@';
            break;
        case 59:
            punct = ';';
            break;


        default:

            var e = [ 'unknown punctuation type: ', punct, new TextDecoder("utf8").decode(m.data) ];
            console.error(e);
            return e;
    }

    const pri = d.getFloat32(j); j+=4;
    const dur = d.getFloat32(j); j+=4;
    const qua = d.getFloat32(j); j+=4;

    const whenLow = d.getInt32(j); j+=4;
    const whenHigh = d.getInt32(j); j+=4;
    const when = whenLow | (whenHigh << 32);

    let freq, conf;
    if ((punct == '.') || (punct == '!')) {
        freq = d.getFloat32(j); j+=4;
        conf = d.getFloat32(j); j+=4;
    } else {
        freq = conf = undefined;
    }

    const term = new TextDecoder("utf8").decode(m.slice(j));

    //console.log(term, punct, pri, when, freq, conf);

    return {
        term: term,
        punc: punct,
        pri: pri,
        dur: dur,
        qua: qua,
        when: when,
        freq: freq,
        conf: conf
    };

    //var uint8array = new TextEncoder(encoding).encode(string);
    //var string = new TextDecoder(encoding).decode(uint8array);

    //var string = new TextDecoder("utf8").decode(m);
    //console.log(string);


    //var width = dv.getUint16(0);
    //var height = dv.getUint16(2);
}

function editify(div) {
    // const editor = ace.edit(div[0]);//"editor");
    // editor.setTheme("ace/theme/vibrant_ink");
    // const LispMode = ace.require("ace/mode/lisp").Mode;
    // editor.session.setMode(new LispMode());
    //
    // editor.$blockScrolling = Infinity;
    //
    // div.css({
    //     position: 'absolute',
    //     width: '100%',
    //     height: '100%'
    // });
    //
    // return editor;

    var t = $(document.createElement('textarea')).appendTo(div);
    return CodeMirror.fromTextArea(t[0], {
        lineNumbers: false,
        theme: 'night',
        mode: 'clojure'
    });
}

function NALEditor(terminal, initialValue) {


    const div = $('<div/>');//.addClass('NALEditor');

    const editor = editify(div);

    editor.setValue(initialValue || ''); //"((a ==> b) <-> x)!\n\necho(\"what\");");

    div.editor = editor;

    editor.on('keypress', function(instance, event) {
        if (event.ctrlKey && event.code==="Enter") {
            const txt = editor.getValue();
            if (txt) {

                try {
                    terminal.send(txt);
                } catch (e) {
                    console.error(e, terminal, txt);
                }

                editor.setValue('');
            }

        }
    });


    div.focus = function() { editor.focus(); return div; };

    // editor.commands.addCommand({
    //     name: 'submit',
    //     bindKey: {win: 'Ctrl-Enter',  mac: 'Ctrl-Enter'},
    //     exec: input,
    //     readOnly: true // false if this command should not apply in readOnly mode
    // });
    //editor.renderer.setShowGutter(false);

    return div;
}



function NARSpeechRecognition(editor) {
    if (!('webkitSpeechRecognition' in window)) {
        console.info('Speech recognition not available');
        return;
    }

    //http://semantic-ui.com/elements/icon.html#audio
    const speechIcon = $('<i class="icon"></i>');

    const speechToggleButton = //$('<button/>').data('record', false);
        //$('<button class="ui inverted icon button"/>');
        $('<button class="ui icon button"></div>').attr('title', 'Record Speech').append(speechIcon);


    const recognition = new webkitSpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;

    let ignore_onend;
    let start_timestamp;
    let recognizing;

    function showInfo(msg) {
        console.log(msg);
    }

    recognition.onstart = function() {
        recognizing = true;
        //showInfo('info_speak_now');
    };
    recognition.onerror = function(event) {
        if (event.error == 'no-speech') {
            alert('no speech');
            ignore_onend = true;
        }
        if (event.error == 'audio-capture') {
            alert('no microphone');
            ignore_onend = true;
        }
        if (event.error == 'not-allowed') {
            if (event.timeStamp - start_timestamp < 100) {
                alert('info_blocked');
            } else {
                alert('info_denied');
            }
            ignore_onend = true;
        }
    };
    recognition.onend = function() {
        recognizing = false;
        if (ignore_onend) {
            return;
        }

        // if (!final_transcript) {
        //     return;
        // }
        //
        // if (window.getSelection) {
        //     window.getSelection().removeAllRanges();
        //     var range = document.createRange();
        //     range.selectNode(document.getElementById('final_span'));
        //     window.getSelection().addRange(range);
        // }

    };
    recognition.onresult = function(event) {
        for (let i = event.resultIndex; i < event.results.length; ++i) {
            const r = event.results[i];
            if (r.isFinal) {
                editor.insert( r[0].transcript );
            }
        }

//         var interim_transcript = '';
//         var new_final_transcript = '';
//         for (var i = event.resultIndex; i < event.results.length; ++i) {
//             if (event.results[i].isFinal) {
//                 new_final_transcript += event.results[i][0].transcript;
//             } else {
//                 interim_transcript += event.results[i][0].transcript;
//             }
//         }
// //console.log(new_final_transcript);
//         final_transcript = capitalize(new_final_transcript);
//         final_span.innerHTML = final_span.innerHTML + linebreak(final_transcript);
//         interim_span.innerHTML = linebreak(interim_transcript);
//         if (final_transcript || interim_transcript) {
//             showButtons('inline-block');
//         }
    };


    function setRecording(r) {
        speechToggleButton.removeClass(r ? "green" : "red");
        speechToggleButton.addClass(r ? "red" : "green");
        speechIcon.removeClass(r ? 'unmute' : 'mute');
        speechIcon.addClass(r ? 'mute' : 'unmute');

        //speechToggleButton.attr('class', r ? 'unmute red icon huge ui button' : 'mute green icon huge ui button');
        if (r) {
            //recognition.lang = select_dialect.value;
            recognition.start();
            ignore_onend = false;
            start_timestamp = event.timeStamp;
        } else {
            recognition.stop();
        }
    }

    setRecording(false);

    return speechToggleButton.click(function() {
        const recording = !speechToggleButton.data('record');

        setRecording(recording);

        speechToggleButton.data('record', recording);
    });

}

function NARInputter(terminal, initialValue) {

    const e = NALEditor(terminal, initialValue);
    const d = $('<div/>').append(
        NARSpeechRecognition(e.editor),

        //other input types..

        e);

    e.editor.renderer.setShowGutter(false);
    
    d.editor = e.editor;

    return d;
    //return d.addClass('ui fluid menu inverted');
}


function NARConsole(terminal, render) {

    const view = $('<div/>');
    const items = $('<div/>').appendTo(view);

    const maxLines = 256;

    let shown = [];

    let queued = false;

    const redraw = () => {

        queued = false;

        fastdom.mutate(()=> {
            const oldItems = items[0];
            const newItems = oldItems.cloneNode(false);
            for (let a of Array.from(shown, render)) {
                if (a) {
                    newItems.appendChild(a);
                }
            }
            view[0].replaceChild(newItems, oldItems);
            items[0] = newItems;

            const height = newItems.scrollHeight;

            setTimeout(() => {
                view.scrollTop(height);
            }, 0);

        });

    };

    terminal.on('message', function(x) {

        //console.log('console: ', JSON.stringify(x), x);


        //const m = JSON.parse(x.data);
        //console.log(x);

        shown.push(x);

        // for (let i = 0; i < x.length; i++) {
        //     shown.push( x[i] );
        // }

        //rr = _.concat(rr, x);
        //rr.push( render(m) );

        const len = shown.length;
        if (len > maxLines) {
            shown = shown.slice(len-maxLines, len-1);
        }


        if (!queued) {

            queued = true;


            setTimeout(redraw, 0);
        }


            // const lines = editor.session.getLength() + newTasks.length;
            // const linesOver = lines - maxLines;
            // if (linesOver > 0) {
            //     editor.session.getDocument().removeFullLines(0, linesOver);
            // }
            //
            // editor.navigateFileEnd();
            // editor.navigateLineEnd();
            //
            // for (let n = Math.max(0, newTasks.length - maxLines); n < newTasks.length; n++) {
            //     if (lines + n > 0)
            //         editor.insert('\n');
            //     editor.navigateLineStart();
            //     editor.insert(line(newTasks[n]));
            // }
            //
            // editor.scrollToRow(editor.getLastVisibleRow());


    });

    return view;

}

function TopTable(path) {
    const e = $('<div/>').attr('class', 'ConceptTable');

    function row(c) {
        const pri = c[1] / 1000.0;
        const dur = c[2] / 1000.0;
        const qua = c[3] / 1000.0;

        const e = document.createElement('span');

        e.className = 'ConceptRow';
        e.style.fontSize = parseInt(70.0 + 2 * 100.0*pri) + '%';
        e.style.color = 'rgb(' +
            parseInt(128 + 128 * pri) + ',' +
            parseInt(64 + 192 * dur) + ',' +
            parseInt(64 + 192 * qua) +
            ')';
        e.innerText = c[0];

        return e;
    }


    return SocketView(path,

        function (p) {
            return e;
        },

        function (msg) {

            setTimeout(()=> {
                const m = JSON.parse(msg.data);

                const rr = [];
                for (let k of m) {
                    rr.push(row(k));
                }

                fastdom.mutate(()=>
                    e.empty().append(rr)
                );

            }, 0);

        }
    );


}
