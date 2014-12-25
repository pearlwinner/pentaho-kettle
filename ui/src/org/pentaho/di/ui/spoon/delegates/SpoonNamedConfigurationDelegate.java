/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.spoon.delegates;

import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.base.HasNamedConfigurationsInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.DBCache;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.UndoInterface;
import org.pentaho.di.core.namedconfig.ConfigurationTemplateManager;
import org.pentaho.di.core.namedconfig.model.NamedConfiguration;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.HasDatabasesInterface;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.namedconfig.dialog.NamedConfigurationDialog;
import org.pentaho.di.ui.spoon.Spoon;

public class SpoonNamedConfigurationDelegate extends SpoonDelegate {
  private static Class<?> PKG = Spoon.class; // for i18n purposes, needed by Translator2!!

  public SpoonNamedConfigurationDelegate( Spoon spoon ) {
    super( spoon );
  }

  public void delNamedConfiguration( HasNamedConfigurationsInterface hasNamedConfigurationsInterface, NamedConfiguration configuration ) {
    int pos = hasNamedConfigurationsInterface.indexOfNamedConfiguration( configuration );
    boolean worked = false;

    // delete from repository?
    Repository rep = spoon.getRepository();
    if ( rep != null ) {
      if ( !rep.getSecurityProvider().isReadOnly() ) {
        try {
          rep.deleteNamedConfiguration( configuration.getName() );
          worked = true;
        } catch ( KettleException dbe ) {
          new ErrorDialog( spoon.getShell(),
            BaseMessages.getString( PKG, "Spoon.Dialog.ErrorDeletingNamedConfiguration.Title" ),
            BaseMessages.getString( PKG, "Spoon.Dialog.ErrorDeletingNamedConfiguration.Message", configuration.getName() ), dbe );
        }
      } else {
        new ErrorDialog( spoon.getShell(),
          BaseMessages.getString( PKG, "Spoon.Dialog.ErrorDeletingNamedConfiguration.Title" ),
          BaseMessages.getString( PKG, "Spoon.Dialog.ErrorDeletingNamedConfiguration.Message", configuration.getName() ),
          new KettleException( BaseMessages.getString( PKG, "Spoon.Dialog.Exception.ReadOnlyUser" ) ) );
      }
    }

    if ( spoon.getRepository() == null || worked ) {
      spoon.addUndoDelete(
        (UndoInterface) hasNamedConfigurationsInterface, new NamedConfiguration[] { (NamedConfiguration) configuration.clone() },
        new int[] { pos } );
      hasNamedConfigurationsInterface.removeNamedConfiguration( pos );
      DBCache.getInstance().clear( configuration.getName() ); // remove this from the cache as well.
    }

    spoon.refreshTree();
    spoon.setShellText();
  }
  
  public void editNamedConfiguration( HasNamedConfigurationsInterface hasNamedConfigurationsInterface, NamedConfiguration configuration, Shell shell ) {
    if ( hasNamedConfigurationsInterface == null && spoon.rep == null ) {
      return;
    }
    NamedConfiguration editingConfiguration = configuration.clone();
    NamedConfigurationDialog namedConfigurationDialog = new NamedConfigurationDialog( shell , editingConfiguration);
    String result = namedConfigurationDialog.open();
    if ( result != null ) {
      if ( !configuration.getName().equals( editingConfiguration.getName() ) ) {
        // name changed, delete old
        delNamedConfiguration( hasNamedConfigurationsInterface, editingConfiguration );
      }
      configuration.replaceMeta( editingConfiguration );
      saveNamedConfiguration( configuration, Const.VERSION_COMMENT_EDIT_VERSION );
      spoon.refreshTree();
    }    
  }

  public void newNamedConfiguration( HasNamedConfigurationsInterface hasNamedConfigurationsInterface, Shell shell) {
    if ( hasNamedConfigurationsInterface == null && spoon.rep == null ) {
      return;
    }
    
    List<NamedConfiguration> configurations = ConfigurationTemplateManager.getInstance().getConfigurationTemplates( "hadoop-cluster" );
    NamedConfiguration configuration = configurations.get( 0 );
    
    NamedConfigurationDialog namedConfigurationDialog = new NamedConfigurationDialog( shell , configuration);
    String result = namedConfigurationDialog.open();
    
    if ( result != null ) {
      if ( hasNamedConfigurationsInterface instanceof VariableSpace ) {
        configuration.shareVariablesWith( (VariableSpace) hasNamedConfigurationsInterface );
      } else {
        configuration.initializeVariablesFrom( null );
      }
  
      hasNamedConfigurationsInterface.addNamedConfiguration( configuration );
      spoon.addUndoNew( (UndoInterface) hasNamedConfigurationsInterface, new NamedConfiguration[] { (NamedConfiguration) configuration.clone() }, 
          new int[] { hasNamedConfigurationsInterface.indexOfNamedConfiguration( configuration ) } );

      saveNamedConfiguration( configuration, Const.VERSION_COMMENT_INITIAL_VERSION );
      
      spoon.refreshTree();    
    }
  }
  
  
  
  
  
  
  public void saveNamedConfiguration( NamedConfiguration configuration, String versionComment ) {
    // Also add to repository?
    Repository rep = spoon.getRepository();

    if ( rep != null ) {
      if ( !rep.getSecurityProvider().isReadOnly() ) {
        try {

          if ( Const.isEmpty( versionComment ) ) {
            rep.insertLogEntry( "Saving configuration '" + configuration.getName() + "'" );
          } else {
            rep.insertLogEntry( "Save configuration : " + versionComment );
          }
          rep.save( configuration, versionComment, null );
          spoon.getLog().logDetailed(
            BaseMessages.getString( PKG, "Spoon.Log.SavedNamedConfiguration", configuration.getName() ) );

          configuration.setChanged( false );
        } catch ( KettleException ke ) {
          new ErrorDialog( spoon.getShell(),
            BaseMessages.getString( PKG, "Spoon.Dialog.ErrorSavingNamedConfiguration.Title" ),
            BaseMessages.getString( PKG, "Spoon.Dialog.ErrorSavingNamedConfiguration.Message", configuration.getName() ), ke );
        }
      } else {
        // This repository user is read-only!
        //
        new ErrorDialog(
          spoon.getShell(), BaseMessages.getString( PKG, "Spoon.Dialog.UnableSave.Title" ), BaseMessages
            .getString( PKG, "Spoon.Dialog.ErrorSavingConnection.Message", configuration.getName() ),
          new KettleException( BaseMessages.getString( PKG, "Spoon.Dialog.Exception.ReadOnlyRepositoryUser" ) ) );
      }
    }
  }  
  
  
  
  
  
  
  
  
  
  
}
