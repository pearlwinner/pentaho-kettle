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

package org.pentaho.di.core.namedconfig.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group implements Serializable {

  private static final long serialVersionUID = -4811976567078682986L;
  
  private String name;
  private List<Property> properties;

  public Group() {
    this.properties = new ArrayList<Property>();
  }
  
  public Group( String name ) {
    this();
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public Property addProperty( Property property ) {
    if ( !containsProperty( property ) ) {
      properties.add( property );
    }
    return property;
  }

  public void deleteProperty( String name ) {
    Property property = getProperty( name );
    deleteProperty( property );
  }

  public void deleteProperty( Property property ) {
      properties.remove( property );
  }  
  
  public Property getProperty( String name ) {
    for ( Property property : properties ) {
      if ( property.getName().equals( name ) ) {
        return property;
      }
    }
    return null;
  }

  public List<Property> getProperties() {
    return Collections.unmodifiableList( properties );
  }
  
  public boolean containsProperty( Property property ) {
    return containsProperty( property.getName() );
  }  
  
  public boolean containsProperty( String name ) {
    Property property = getProperty( name );
    return property != null;
  }
}
