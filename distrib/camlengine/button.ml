(* $Id$ *)

(*      This is essentially the same as the qmw button stuff: evidently
        the two interfaces have converged in this area. 
        
        Differences are:
                
                1. I like putting separators in the Edit menus to separate different
                   kinds of editing action.
                
                2. I don't really care to make use of the variability provided
                   by markpanelentry in choosing the mark to put by a 
                   proven conjecture. (Too right: RB.  Now it is a true/false marker)
        BAS
*)

open Menu 
open Mappingfuns
open Panelkind.M
open Name.M
    
type name = Name.M.name

let deadServer = Interaction.deadServer
and runningServer = (fun() -> Optionfuns.M.opt2bool !Japeserver.serverpid)
 
type button = UndoProofbutton
			| RedoProofbutton
			| UndoDisproofbutton
			| RedoDisproofbutton
			| Finishedbutton
			| Resetbutton
			| Savebutton
			| SaveAsbutton
			| Disprovebutton

type mybutton = MyUndoProof
			  | MyRedoProof
			  | MyUndoDisproof
			  | MyRedoDisproof
			  | MyDone
			  | MyClose
			  | MySave
			  | MySaveAs
			  | MyDisprove

let buttoncache : (mybutton, bool ref) mapping ref = ref empty
let rec enable (button, state) =
  let rec doit b v =
    let (m, c) =
      match b with
        MyUndoProof -> "Edit", "Undo Proof Step"
      | MyRedoProof -> "Edit", "Redo Proof Step"
      | MyUndoDisproof -> "Edit", "Undo Disproof Step"
      | MyRedoDisproof -> "Edit", "Redo Disproof Step"
      | MyDone -> "Edit", "Done"
      | MyClose -> "File", "Close"
      | MySave -> "File", "Save"
      | MySaveAs -> "File", "Save As"
      | MyDisprove -> "Edit", "Disprove"
    in
    if match at (!buttoncache, b) with
         Some r ->
           if !r = state then false else begin r := state; true end
       | None ->
           buttoncache := ( ++ ) (!buttoncache, ( |-> ) (b, ref state));
           true
    then
      Japeserver.enablemenuitem (m, c, state)
  in
  match button with
    UndoProofbutton -> doit MyUndoProof state
  | RedoProofbutton -> doit MyRedoProof state
  | UndoDisproofbutton -> doit MyUndoDisproof state
  | RedoDisproofbutton -> doit MyRedoDisproof state
  | Finishedbutton -> doit MyDone state
  | Resetbutton -> doit MyClose state
  | Savebutton -> doit MySave state
  | SaveAsbutton -> doit MySaveAs state
  | Disprovebutton -> doit MyDisprove state
let (resetfontstuff, setfontstuff, getfontstuff) =
  let fontstuff : string option ref = ref None in
  let rec resetfontstuff () = fontstuff := None
  and setfontstuff stuff = fontstuff := Some stuff
  and getfontstuff () = !fontstuff in
  resetfontstuff, setfontstuff, getfontstuff

let rec reloadmenusandpanels markconjecture oplist =
  if runningServer () then
    try
      (* was freshmenus... *)
      Japeserver.sendOperators oplist;
      buttoncache := empty;
      menuiter
        (fun menu ->
           let menustring = namestring menu in
           Japeserver.newmenu menustring;
           menuitemiter menu
             (fun (label, keyopt, cmd) ->
                Japeserver.menuentry
                  (menustring, namestring label, keyopt, cmd))
             (fun (label, cmd) ->
                (* checkbox *)
                Japeserver.menucheckbox
                  (menustring, namestring label, cmd))
             (fun lcs ->
                (* radio button *)
                Japeserver.menuradiobutton
                  (menustring,
                   List.map (fun (label, cmd) -> namestring label, cmd) lcs))
             (fun _ -> Japeserver.menuseparator menustring));
      paneliter
        (fun (panel, kind) ->
           let panelstring = namestring panel in
           Japeserver.newpanel (panelstring, kind);
           panelitemiter panel
             (fun (label, entry) ->
                Japeserver.panelentry
                  (panelstring, namestring label, entry);
                if kind = ConjecturePanelkind then
                  match markconjecture label with
                    Some mark ->
                      Japeserver.markpanelentry panelstring
                        (namestring label) mark
                  | _ -> ())
             (fun (name, cmd) ->
                (* button *)
                Japeserver.panelbutton
                  (panelstring, namestring name, cmd))
             (fun (label, cmd) ->
                (* checkbox *)
                Japeserver.panelcheckbox
                  (panelstring, namestring label, cmd))
             (fun lcs ->
                (* radio button *)
                Japeserver.panelradiobutton
                  (panelstring,
                   List.map (fun (n, v) -> namestring n, v) lcs)));
      Japeserver.mapmenus true;
      let _ = Japeserver.echo "" (* synchronise *) in ()
    with
      Japeserver.DeadServer_ -> deadServer ["WARNING: server broken"]
let rec markproof proved cmd =
  (* given parseable name - look in the cmd part of conjecture panels *)
  paneliter
    (function
       panel, ConjecturePanelkind ->
         panelitemiter panel
           (fun (label, entry) ->
              if entry = cmd then
                Japeserver.markpanelentry (namestring panel)
                  (namestring label) proved)
           (fun _ -> ()) (fun _ -> ()) (fun _ -> ())
     | panel, _ -> ())
let rec initButtons () =
  let ( -------- ) = Mseparator in
  let rec _E (name, cut, cmd) = Mentry (Name.M.Name name, cut, cmd) in
  let _EditEntries =
    [_E ("Done", Some "D", "done"); ( -------- );
     _E ("Undo Proof Step", None, "undo_proof");
     _E ("Redo Proof Step", None, "redo_proof"); ( -------- );
     _E ("Undo Disproof Step", None, "undo_disproof");
     _E ("Redo Disproof Step", None, "redo_disproof"); ( -------- );
     _E ("Backtrack", None, "backtrack");
     _E ("Prune", None, "prune"); ( -------- );
     _E ("Unify selected terms", None, "unify"); ( -------- );
     _E ("Refresh", None, "refreshdisplay"); ( -------- );
     _E ("Hide/Show subproof", None, "collapse");
     _E ("Expand/Contract detail", None, "layout")]
  in
  clearmenusandpanels ();
  addmenu (Name.M.Name "Edit");
  addmenudata (Name.M.Name "Edit") _EditEntries
let rec initFonts () =
  match getfontstuff () with
    Some stuff -> Japeserver.setFonts stuff
  | None -> ()













