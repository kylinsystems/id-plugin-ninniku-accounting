/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package tw.ninniku.accounting.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.util.CLogger;
import org.compiere.util.DB;


public class ProductAcct
{

	/**	Logger					*/
	private static CLogger 	log = CLogger.getCLogger (ProductAcct.class);

	public static MAccount getAccout(int m_Product_ID, String accountName, MAcctSchema as) {
		int validCombination_ID = 0;
		String sql = "SELECT * "									//  24
				+ "FROM M_Product_Acct "
				+ "WHERE M_Product_ID=? AND C_AcctSchema_ID=?";
			//
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, as.get_TrxName());
				pstmt.setInt(1, m_Product_ID);
				pstmt.setInt(2, as.getC_AcctSchema_ID());
				rs = pstmt.executeQuery();

				if (rs.next())
					validCombination_ID = rs.getInt(accountName);
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql, e);
			}
			finally {
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
			return MAccount.get(as.getCtx(), validCombination_ID);
	}
}	//	ProductCost
