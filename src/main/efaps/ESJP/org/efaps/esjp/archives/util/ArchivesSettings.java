/*
 * Copyright 2003 - 2013 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */


package org.efaps.esjp.archives.util;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("35d00ada-2336-4c3a-bbf9-aed20d3bcee8")
@EFapsRevision("$Rev$")
public interface ArchivesSettings
{

    /**
     * Boolean (true/false).
     * Activate the possibility to change file to store.
     */
    String ACTIVATE_EDITFILENAME = "org.efaps.archives.ActivateEditFileName";

    /**
     * Boolean (true/false).
     * Activate versioning of archives
     */
    String ACTIVATE_VERSIONING = "org.efaps.archives.ActivateVersioning";

}
