/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */

package org.efaps.esjp.archives.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("17152521-af16-45af-ad51-99d2cdc8c9bd")
@EFapsApplication("eFapsApp-Archives")
@EFapsSystemConfiguration("18c6c5fb-fa59-4951-8f67-fc6644dccb44")
public final class Archives
{

    /** The base. */
    public static final String  BASE = "org.efaps.archives.";

    /** Archives-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("18c6c5fb-fa59-4951-8f67-fc6644dccb44");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEEDITFILENAME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ActivateEditFileName")
                    .description("Activate thee diting of file names.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEVERSIONING = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ActivateVersioning")
                    .description("Activate versioning of archives.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute OBJ2ARCHCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4Object2Archive")
                    .concatenate(true)
                    .description("Config for Object to Archive relations. e.g.\n"
                                    + "SalesInvoice.DefaultRole=ROLE\nSalesInvoice.DefaultAccessSet=ACCESSSET\n"
                                    + "SalesInvoice.Object2ArchiveType=RELATIONTYPE\n"
                                    + "SalesInvoice.CreateRoot=true\n"
                                    + "SalesInvoice.RootName=ROOTNAME\n"
                                    + "SalesInvoice.Folder=FOLDERNAME\n"
                                    + "SalesInvoice.Folder01=FOLDERNAME/FOLDERNAME\n");

    /**
     * Singelton.
     */
    private Archives()
    {
    }

    /**
     * @return the SystemConfigruation for Archives
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        // Archives-Configuration
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
