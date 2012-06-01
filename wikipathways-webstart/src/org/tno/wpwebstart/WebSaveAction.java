package org.tno.wpwebstart;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.Pathway.StatusFlagEvent;
import org.pathvisio.core.model.Pathway.StatusFlagListener;
import org.pathvisio.core.util.Resources;
import org.tno.wpwebstart.WikiPathwaysHandler.Parameter;

public class WebSaveAction extends AbstractAction implements StatusFlagListener, ApplicationEventListener {
	WikiPathwaysHandler wpHandler;

	public WebSaveAction(WikiPathwaysHandler wpHandler) {
		super("Save to " + wpHandler.getParameter(Parameter.siteName), new ImageIcon(Resources.getResourceURL("savetoweb.gif")));

		this.wpHandler = wpHandler;
		wpHandler.getDesktop().getSwingEngine().getEngine().addApplicationEventListener(this);
		putValue(Action.SHORT_DESCRIPTION, "Save the pathway online to " + wpHandler.getParameter(Parameter.siteName));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			wpHandler.saveWithProgress();
		} catch(Exception ex) {
			Logger.log.error("Unable to save pathway", ex);
			JOptionPane.showMessageDialog(null, "Unable to save pathway:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void statusFlagChanged(StatusFlagEvent e) {
		setEnabled(e.getNewStatus());
	}

	@Override
	public void applicationEvent(org.pathvisio.core.ApplicationEvent e) {
		switch (e.getType()) {
		case PATHWAY_NEW:
		case PATHWAY_OPENED:
			Pathway p = wpHandler.getDesktop().getSwingEngine().getEngine().getActivePathway();
			p.addStatusFlagListener(this);
			setEnabled(true);
		}
	}
}