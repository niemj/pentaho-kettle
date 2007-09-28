 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/

 
package be.ibridge.kettle.repository.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import be.ibridge.kettle.core.GUIResource;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.LogWriter;
import be.ibridge.kettle.core.Props;
import be.ibridge.kettle.core.WindowProperty;
import be.ibridge.kettle.core.database.DatabaseMeta;
import be.ibridge.kettle.core.dialog.DatabaseDialog;
import be.ibridge.kettle.core.dialog.EnterPasswordDialog;
import be.ibridge.kettle.core.dialog.ErrorDialog;
import be.ibridge.kettle.core.exception.KettleDatabaseException;
import be.ibridge.kettle.core.exception.KettleException;
import be.ibridge.kettle.repository.RepositoriesMeta;
import be.ibridge.kettle.repository.Repository;
import be.ibridge.kettle.repository.RepositoryMeta;
import be.ibridge.kettle.repository.UserInfo;
import be.ibridge.kettle.trans.StepLoader;
import be.ibridge.kettle.trans.step.BaseStepDialog;


/**
 * This dialog is used to edit/enter the name and description of a repository.
 * You can also use this dialog to create, upgrade or remove a repository.
 * 
 * @author Matt
 * @since 19-jun-2003
 *
 */

public class RepositoryDialog
{
	private LogWriter    log;

	private Label        wlConnection;
	private Button       wnConnection, weConnection, wdConnection;
	private CCombo       wConnection;
	private FormData     fdlConnection, fdConnection, fdnConnection, fdeConnection, fddConnection;

	private Label        wlName;
	private Text         wName;
	private FormData     fdlName, fdName;

	private Label        wlDescription;
	private Text         wDescription;
	private FormData     fdlDescription, fdDescription;

	private Button wOK, wCreate, wDrop, wCancel;
    private Listener lsOK, lsCreate, lsDrop, lsCancel;

	private Display       display;
	private Shell         shell, parent;
	private Props         props;
	
	private RepositoryMeta   input;
	private RepositoriesMeta repositories;
	private StepLoader       steploader;
	
	public RepositoryDialog(Shell par, int style, LogWriter l, Props pr, RepositoryMeta in, RepositoriesMeta rep, StepLoader steploader)
	{
		parent  = par;
		display = parent.getDisplay();
		this.steploader = steploader;
		shell = new Shell(display, style | SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
		shell.setText(Messages.getString("RepositoryDialog.Dialog.Main.Title")); //$NON-NLS-1$

		log=l;
		props=pr;
		input=in;
		repositories=rep;
		
		//System.out.println("input.connection = "+input.getConnection());
	}

	public RepositoryMeta open()
	{
		props.setLook(shell);

		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setImage(GUIResource.getInstance().getImageSpoon());
		shell.setText(Messages.getString("RepositoryDialog.Dialog.Main.Title2")); //$NON-NLS-1$
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Add the connection buttons :
		wnConnection = new Button(shell, SWT.PUSH);  wnConnection.setText(Messages.getString("System.Button.New")); //$NON-NLS-1$
		weConnection = new Button(shell, SWT.PUSH);  weConnection.setText(Messages.getString("System.Button.Edit")); //$NON-NLS-1$
		wdConnection = new Button(shell, SWT.PUSH);  wdConnection.setText(Messages.getString("System.Button.Delete")); //$NON-NLS-1$

		// Button positions...
		fddConnection = new FormData();		
		fddConnection.right= new FormAttachment(100, 0);
		fddConnection.top  = new FormAttachment(0, margin);
		wdConnection.setLayoutData(fddConnection);

		fdeConnection = new FormData();		
		fdeConnection.right= new FormAttachment(wdConnection, -margin);
		fdeConnection.top  = new FormAttachment(0, margin);
		weConnection.setLayoutData(fdeConnection);

		fdnConnection = new FormData();		
		fdnConnection.right = new FormAttachment(weConnection, -margin);
		fdnConnection.top   = new FormAttachment(0, margin);
		wnConnection.setLayoutData(fdnConnection);

		wConnection=new CCombo(shell, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
 		props.setLook(wConnection);
		fdConnection=new FormData();
		fdConnection.left = new FormAttachment(middle, 0);
		fdConnection.top  = new FormAttachment(wnConnection, 0, SWT.CENTER);
		fdConnection.right= new FormAttachment(wnConnection, -margin);
		wConnection.setLayoutData(fdConnection);

		fillConnections();		

        // Connection line
        wlConnection=new Label(shell, SWT.RIGHT);
        wlConnection.setText(Messages.getString("RepositoryDialog.Label.SelectConnection")); //$NON-NLS-1$
        props.setLook(wlConnection);
        fdlConnection=new FormData();
        fdlConnection.left = new FormAttachment(0, 0);
        fdlConnection.right= new FormAttachment(middle, -margin);
        fdlConnection.top  = new FormAttachment(wnConnection, 0, SWT.CENTER);
        wlConnection.setLayoutData(fdlConnection);



		
		// Add the listeners
		// New connection
		wnConnection.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent arg0) 
				{
					DatabaseMeta databaseMeta = new DatabaseMeta();
					DatabaseDialog dd = new DatabaseDialog(shell, databaseMeta);
					if (dd.open()!=null)
					{
						repositories.addDatabase(databaseMeta);
						fillConnections();
						
						int idx = repositories.indexOfDatabase(databaseMeta);
						wConnection.select(idx);
						
					}
				}
			}
		);
	
		// Edit connection
		weConnection.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent arg0) 
				{
					DatabaseMeta databaseMeta = repositories.searchDatabase(wConnection.getText());
					if (databaseMeta!=null)
					{
						DatabaseDialog dd = new DatabaseDialog(shell, databaseMeta);
						if (dd.open()!=null)
						{
							fillConnections();
							int idx = repositories.indexOfDatabase(databaseMeta);
							wConnection.select(idx);
						}
					}
				}
			}
		);
	
		// Delete connection
		wdConnection.addSelectionListener(new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent arg0) 
				{
					DatabaseMeta dbinfo = repositories.searchDatabase(wConnection.getText());
					if (dbinfo!=null)
					{
						int idx = repositories.indexOfDatabase(dbinfo);
						repositories.removeDatabase(idx);
						fillConnections();
					}
				}
			}
		);
		
		// Name line
		wlName=new Label(shell, SWT.RIGHT);
		wlName.setText(Messages.getString("RepositoryDialog.Label.Name")); //$NON-NLS-1$
 		props.setLook(wlName);
		fdlName=new FormData();
		fdlName.left = new FormAttachment(0, 0);
		fdlName.top  = new FormAttachment(wnConnection, margin*2);
		fdlName.right= new FormAttachment(middle, -margin);
		wlName.setLayoutData(fdlName);
		wName=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wName);
		fdName=new FormData();
		fdName.left = new FormAttachment(middle, 0);
		fdName.top  = new FormAttachment(wnConnection, margin*2);
		fdName.right= new FormAttachment(100, 0);
		wName.setLayoutData(fdName);

		// Description line
		wlDescription=new Label(shell, SWT.RIGHT);
		wlDescription.setText(Messages.getString("RepositoryDialog.Label.Description")); //$NON-NLS-1$
 		props.setLook(wlDescription);
		fdlDescription=new FormData();
		fdlDescription.left = new FormAttachment(0, 0);
		fdlDescription.top  = new FormAttachment(wName, margin);
		fdlDescription.right= new FormAttachment(middle, -margin);
		wlDescription.setLayoutData(fdlDescription);
		wDescription=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
 		props.setLook(wDescription);
		fdDescription=new FormData();
		fdDescription.left = new FormAttachment(middle, 0);
		fdDescription.top  = new FormAttachment(wName, margin);
		fdDescription.right= new FormAttachment(100, 0);
		wDescription.setLayoutData(fdDescription);


		

		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK")); //$NON-NLS-1$
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		wOK.addListener    (SWT.Selection, lsOK    );

		wCreate=new Button(shell, SWT.PUSH);
		wCreate.setText(Messages.getString("RepositoryDialog.Button.CreateOrUpgrade")); //$NON-NLS-1$
		lsCreate   = new Listener() { public void handleEvent(Event e) { create(steploader); } };
		wCreate.addListener(SWT.Selection, lsCreate);

		wDrop=new Button(shell, SWT.PUSH);
		wDrop.setText(Messages.getString("RepositoryDialog.Button.Remove")); //$NON-NLS-1$
		lsDrop     = new Listener() { public void handleEvent(Event e) { drop();   } };
		wDrop.addListener  (SWT.Selection, lsDrop  );

		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel")); //$NON-NLS-1$
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		wCancel.addListener(SWT.Selection, lsCancel);

        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCreate, wDrop, wCancel}, margin, wDescription);
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		getData();

		BaseStepDialog.setSize(shell);

		shell.open();
		while (!shell.isDisposed())
		{
				if (!display.readAndDispatch()) display.sleep();
		}
		return input;
	}

	public void dispose()
	{
		props.setScreen(new WindowProperty(shell));
		shell.dispose();
	}
	
	/**
	 * Copy information from the meta-data input to the dialog fields.
	 */ 
	public void getData()
	{
		if (input.getName()!=null) wName.setText(input.getName());
		if (input.getDescription()!=null) wDescription.setText(input.getDescription());
		if (input.getConnection()!=null) wConnection.setText(input.getConnection().getName());	
	}
	
	private void cancel()
	{
		input = null;
		dispose();
	}
	
	private void getInfo(RepositoryMeta info)
	{
		info.setName(wName.getText());
		info.setDescription(wDescription.getText());
		
		int idx = wConnection.getSelectionIndex();
		if (idx>=0)
		{
			DatabaseMeta dbinfo = repositories.getDatabase(idx);
            info.setConnection(dbinfo);
		}
		else
		{
			info.setConnection(null);
		}
	}
	
	private void ok()
	{
		getInfo(input);
        
        if (input.getName()!=null && input.getName().length()>0)
        {
            dispose();
        }
        else
        {
            MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK );
            box.setMessage(Messages.getString("RepositoryDialog.Dialog.ErrorNoName.Message")); //$NON-NLS-1$
            box.setText(Messages.getString("RepositoryDialog.Dialog.ErrorNoName.Title")); //$NON-NLS-1$
            box.open();
       }
	}
	
	private void fillConnections()
	{
		wConnection.removeAll();
		
		// Fill in the available connections...
		for (int i=0;i<repositories.nrDatabases();i++) wConnection.add( repositories.getDatabase(i).getName() );
	}
	
	private void create(StepLoader steploader)
	{
		System.out.println("Loading repository info..."); //$NON-NLS-1$
		
		RepositoryMeta repinfo = new RepositoryMeta();
		getInfo(repinfo);
		
		if (repinfo.getConnection()!=null)
		{
            if (repinfo.getConnection().getAccessType()==DatabaseMeta.TYPE_ACCESS_ODBC)
            {
                // Show a warning: using ODBC is not always the best choice ;-)
                System.out.println("Show ODBC warning..."); //$NON-NLS-1$
                
                MessageBox qmb = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
                qmb.setMessage(Messages.getString("RepositoryDialog.Dialog.ODBCIsNotSafe.Message", Const.CR, Const.CR)); //$NON-NLS-1$ //$NON-NLS-2$
                qmb.setText(Messages.getString("RepositoryDialog.Dialog.ODBCIsNotSafe.Title")); //$NON-NLS-1$
                int answer = qmb.open();    
                if (answer!=SWT.YES)
                {
                    return; // Don't continue
                }

            }
			System.out.println("Allocating repository..."); //$NON-NLS-1$
			Repository rep = new Repository(log, repinfo, null);
	
			System.out.println("Connecting to database for repository creation..."); //$NON-NLS-1$
			try
			{
                rep.connect(true, false, getClass().getName(), true);
				boolean upgrade=false;
				String  cu = Messages.getString("RepositoryDialog.Dialog.CreateUpgrade.Create"); //$NON-NLS-1$
				
				try
				{
					upgrade = rep.getDatabase().checkTableExists("R_USER"); //$NON-NLS-1$
					if (upgrade) cu=Messages.getString("RepositoryDialog.Dialog.CreateUpgrade.Upgrade"); //$NON-NLS-1$
				}
				catch(KettleDatabaseException dbe)
				{
				    rep.rollback();
				    // Don't show the error anymore, just go ahead and create the damn thing!
					// ErrorDialog ed = new ErrorDialog(shell, props, "ERROR", "An unexpected error occured trying to check the existence of table R_USER in the repository."+Const.CR+"We consider the table to be non-existent and we will try to create the repository. ", dbe);
				}
				
				MessageBox qmb = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
				qmb.setMessage(Messages.getString("RepositoryDialog.Dialog.CreateUpgrade.Message1")+cu+Messages.getString("RepositoryDialog.Dialog.CreateUpgrade.Message2")); //$NON-NLS-1$ //$NON-NLS-2$
				qmb.setText(Messages.getString("RepositoryDialog.Dialog.CreateUpgrade.Title")); //$NON-NLS-1$
				int answer = qmb.open();	
				
				if (answer == SWT.YES)
				{
					boolean goAhead = !upgrade;
					
					if (!goAhead)
					{
						EnterPasswordDialog etd = new EnterPasswordDialog(shell, Messages.getString("RepositoryDialog.Dialog.EnterPassword.Title"), Messages.getString("RepositoryDialog.Dialog.EnterPassword.Message"), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						String pwd = etd.open();
						if (pwd!=null)
						{
							try
							{
								// OK, what's the admin password?
								UserInfo ui = new UserInfo(rep, "admin"); //$NON-NLS-1$
								
								if (pwd.equalsIgnoreCase( ui.getPassword() ) )
								{
									goAhead=true;
								}
								else
								{
									MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
									mb.setMessage(Messages.getString("RepositoryDialog.Dialog.UpgradePasswordNotCorrect.Message")); //$NON-NLS-1$
									mb.setText(Messages.getString("RepositoryDialog.Dialog.UpgradePasswordNotCorrect.Title")); //$NON-NLS-1$
									mb.open();
								}
							}
							catch(KettleException e)
							{
								new ErrorDialog(shell, Messages.getString("RepositoryDialog.Dialog.UnableToVerifyUser.Title"), Messages.getString("RepositoryDialog.Dialog.UnableToVerifyUser.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					}
					
					if (goAhead)
					{
						System.out.println(Messages.getString("RepositoryDialog.Dialog.TryingToUpgradeRepository.Message1")+cu+Messages.getString("RepositoryDialog.Dialog.TryingToUpgradeRepository.Message2")); //$NON-NLS-1$ //$NON-NLS-2$
						UpgradeRepositoryProgressDialog urpd = new UpgradeRepositoryProgressDialog(shell, rep, upgrade);
						if (urpd.open())
						{
							MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
							mb.setMessage(Messages.getString("RepositoryDialog.Dialog.UpgradeFinished.Message1")+cu+Messages.getString("RepositoryDialog.Dialog.UpgradeFinished.Message2")); //$NON-NLS-1$ //$NON-NLS-2$
							mb.setText(Messages.getString("RepositoryDialog.Dialog.UpgradeFinished.Title")); //$NON-NLS-1$
							mb.open();
						}
					}
				}

				rep.disconnect();
			}
			catch(KettleException ke)
			{
                new ErrorDialog(shell, Messages.getString("RepositoryDialog.Dialog.UnableToConnectToUpgrade.Title"), Messages.getString("RepositoryDialog.Dialog.UnableToConnectToUpgrade.Message")+Const.CR, ke); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		else
		{
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setMessage(Messages.getString("RepositoryDialog.Dialog.FirstCreateAValidConnection.Message")); //$NON-NLS-1$
			mb.setText(Messages.getString("RepositoryDialog.Dialog.FirstCreateAValidConnection.Title")); //$NON-NLS-1$
			mb.open();
		}
	}

	private void drop()
	{
		RepositoryMeta repinfo = new RepositoryMeta();
		getInfo(repinfo);
		
		Repository rep = new Repository(log, repinfo, null);
		try
		{
            rep.connect(getClass().getName());
            
			MessageBox qmb = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
			qmb.setMessage(Messages.getString("RepositoryDialog.Dialog.ConfirmRemovalOfRepository.Message")); //$NON-NLS-1$
			qmb.setText(Messages.getString("RepositoryDialog.Dialog.ConfirmRemovalOfRepository.Title")); //$NON-NLS-1$
			int answer = qmb.open();
			
			if (answer == SWT.YES)
			{
				EnterPasswordDialog etd = new EnterPasswordDialog(shell, Messages.getString("RepositoryDialog.Dialog.AskAdminPassword.Title"), Messages.getString("RepositoryDialog.Dialog.AskAdminPassword.Message"), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				String pwd = etd.open();
				if (pwd!=null)
				{
					try
					{
						// OK, what's the admin password?
						UserInfo ui = new UserInfo(rep, "admin"); //$NON-NLS-1$
						
						if (pwd.equalsIgnoreCase( ui.getPassword() ) )
						{		
							try
							{
								rep.dropRepositorySchema();
								
								MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
								mb.setMessage(Messages.getString("RepositoryDialog.Dialog.RemovedRepositoryTables.Message")); //$NON-NLS-1$
								mb.setText(Messages.getString("RepositoryDialog.Dialog.RemovedRepositoryTables.Title")); //$NON-NLS-1$
								mb.open();
							}
							catch(KettleDatabaseException dbe)
							{
								MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
								mb.setMessage(Messages.getString("RepositoryDialog.Dialog.UnableToRemoveRepository.Message")+Const.CR+dbe.getMessage()); //$NON-NLS-1$
								mb.setText(Messages.getString("RepositoryDialog.Dialog.UnableToRemoveRepository.Title")); //$NON-NLS-1$
								mb.open();
							}
						}
						else
						{
							MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
							mb.setMessage(Messages.getString("RepositoryDialog.Dialog.UnableToRemoveRepositoryPwdIncorrect.Message")); //$NON-NLS-1$
							mb.setText(Messages.getString("RepositoryDialog.Dialog.UnableToRemoveRepositoryPwdIncorrect.Title")); //$NON-NLS-1$
							mb.open();
						}
					}
					catch(KettleException e)
					{
						new ErrorDialog(shell, Messages.getString("RepositoryDialog.Dialog.UnableToVerifyAdminUser.Title"), Messages.getString("RepositoryDialog.Dialog.UnableToVerifyAdminUser.Message"), e); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			rep.disconnect();
		}
		catch(KettleException ke)
		{
            new ErrorDialog(shell, Messages.getString("RepositoryDialog.Dialog.NoRepositoryFoundOnConnection.Title"), Messages.getString("RepositoryDialog.Dialog.NoRepositoryFoundOnConnection.Message"), ke); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
