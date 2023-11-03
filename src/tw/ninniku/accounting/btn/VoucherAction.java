package tw.ninniku.accounting.btn;

import org.adempiere.util.IProcessUI;
import org.adempiere.util.ProcessUtil;
import org.adempiere.webui.action.IAction;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.adwindow.ADWindowContent;
import org.compiere.model.GridTab;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.util.*;
public class VoucherAction implements IAction{

	private int ad_table_id;
	private int record_id;

	@Override
	public void execute(Object target) {
		// TODO Auto-generated method stub
		
		ADWindow window = (ADWindow) target;
		ADWindowContent content = window.getADWindowContent();
		GridTab tab = content.getActiveGridTab();
		
		ad_table_id = tab.getAD_Table_ID();
		record_id = tab.getRecord_ID();
		
		//String tableName  = MTable.getTableName(Env.getCtx(), ad_table_id);
		//System.out.print("tablename:" + tableName +"ad_table_id:" + ad_table_id + " record_id:" + record_id );
		
		validDocument(ad_table_id,record_id);
		
		printReport();
	}


	private void validDocument(int ad_table_id, int record_id) {
		// TODO Auto-generated method stub
		MTable mTable  = 	MTable.get(Env.getCtx(), ad_table_id);
		PO po = mTable.getPO(record_id, null);
		int ii = po.get_ColumnIndex("Posted");
		if(po.get_ColumnIndex("Posted") >= 0)
		{
			//String posted =   po.get_ValueAsString("Posted");
			if(!po.get_ValueAsBoolean("Posted"))
			{
				throw new AdempiereUserError(  Msg.parseTranslation(Env.getCtx(), "@"+po.get_TableName()+"_ID@"  + "未過帳！"));
			}
		}else {
			
			throw new AdempiereUserError( "本文件無傳票功能！");

		}
	}


	private void printReport() {
		// TODO Auto-generated method stub
		int AD_Process_ID = MProcess.getProcess_ID("ACCOUNTING_FACT_JR",null);
		MProcess process = MProcess.get(Env.getCtx(), AD_Process_ID);
		MPInstance pInstance = new MPInstance(process, record_id);
		pInstance.createParameter(10, "AD_Table_ID", ad_table_id);
		pInstance.createParameter(20, "RECORD_ID", record_id);


		ProcessInfo pi = new ProcessInfo("ACCOUNTING_FACT_JR",
				AD_Process_ID,ad_table_id, record_id);
		pi.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
		pi.setAD_Client_ID(Env.getAD_Client_ID(Env.getCtx()));
		pi.setAD_PInstance_ID(pInstance.getAD_PInstance_ID());
		pi.setRecord_ID(record_id);
		
		ArrayList<ProcessInfoParameter> jasperPrintParams = new ArrayList<ProcessInfoParameter>();
		ProcessInfoParameter pip;

		pip = new ProcessInfoParameter("AD_PInstance_ID", pInstance.getAD_PInstance_ID(), null, null, null);
		jasperPrintParams.add(pip);
		
		pip = new ProcessInfoParameter("AD_Table_ID", ad_table_id, null, null, null);
		jasperPrintParams.add(pip);
		
		pip = new ProcessInfoParameter("RECORD_ID", record_id, null, null, null);
		jasperPrintParams.add(pip);
		
		Properties p =  Env.getCtx();
		pip = new ProcessInfoParameter("CURRENT_LANG", p.getProperty("#Locale"), null, null, null);
		jasperPrintParams.add(pip);
		
        /**
         * 
         */
		Trx trx = Trx.get(Trx.createTrxName(), true);
		trx.start();
		String sql = "   SELECT DateAcct  "
				+ "   FROM Fact_Acct  fa "
				+ "   WHERE (AD_Table_ID = ? AND Record_ID = ? ) ;";
		Timestamp dateAcct = DB.getSQLValueTS(trx.getTrxName(), sql, new Object[] {ad_table_id,record_id});
		
		sql = " select fi_get_fact_documentno(?,?,?) ";
		String factno = DB.getSQLValueString(trx.getTrxName(), sql, new Object[] {ad_table_id,record_id,dateAcct});
		//CURRENT_LANG
		pi.setParameter(jasperPrintParams.toArray(new ProcessInfoParameter[]{}));
		pi.setPrintPreview(true);
		trx.commit();
		trx.close();
		trx = Trx.get(Trx.createTrxName(), true);
		trx.start();
	    // CarlosRuiz - globalqss - allow procedure preprocess
	    if (process.getProcedureName() != null && process.getProcedureName().length() > 0) {
			//  execute on this thread/connection
			 sql = "{call " + process.getProcedureName() + "(?)}";
			CallableStatement cstmt = null;
			try
			{
				cstmt = DB.prepareCall(sql,1,trx.getTrxName());	//	ro??
				cstmt.setInt(1, pi.getAD_PInstance_ID());
				cstmt.executeUpdate();
			}
			catch (Exception e)
			{
				System.out.println( e.getLocalizedMessage());
			}
			finally
			{
				DB.close(cstmt);
				cstmt = null;
//				trx.commit();
//				trx.close();
			}
	    }
//	    trx = Trx.get(Trx.createTrxName(), true);
//		trx.start();
			IProcessUI processMonitor = Env.getProcessUI(Env.getCtx());
			ProcessUtil.startJavaProcess(Env.getCtx(), pi, trx, false,processMonitor);

		trx.commit();
		trx.close();
	}
	
}
