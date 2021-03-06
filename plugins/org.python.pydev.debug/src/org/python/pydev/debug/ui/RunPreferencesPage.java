package org.python.pydev.debug.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;

public class RunPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public RunPreferencesPage() {
        super("Run", GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        BooleanFieldEditor editor = new BooleanFieldEditor(
                PyDevEditorPreferences.KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS,
                "When terminating process, kill subprocesses too?", BooleanFieldEditor.SEPARATE_LABEL,
                p);
        Control c = editor.getDescriptionControl(p);
        c.setToolTipText("When this option is turned on, terminating a launch will also terminate subprocesses.");
        addField(editor);

        editor = new BooleanFieldEditor(PyDevEditorPreferences.MAKE_LAUNCHES_WITH_M_FLAG,
                "Launch modules with \"python -m mod.name\" instead of \"python filename.py\"?",
                BooleanFieldEditor.SEPARATE_LABEL,
                p);
        c = editor.getDescriptionControl(p);
        c.setToolTipText(
                "When this option is turned on, any launch will launch using the \"-m\" flag (which allows relative imports in the main module).");
        addField(editor);

    }

    public static boolean getKillSubprocessesWhenTerminatingProcess() {
        return PydevPrefs.getEclipsePreferences().getBoolean(
                PyDevEditorPreferences.KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS,
                PyDevEditorPreferences.DEFAULT_KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS);
    }

    public static boolean getLaunchWithMFlag() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.MAKE_LAUNCHES_WITH_M_FLAG,
                PyDevEditorPreferences.DEFAULT_MAKE_LAUNCHES_WITH_M_FLAG);
    }

}
