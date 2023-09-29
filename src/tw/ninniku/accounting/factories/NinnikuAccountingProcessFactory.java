package tw.ninniku.accounting.factories;
import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import tw.ninniku.accounting.process.CopyDefaultProductAccouning;
public class NinnikuAccountingProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		if(className == null)
			return null;
		if(className.equals(CopyDefaultProductAccouning.class.getName()))
			return new CopyDefaultProductAccouning();
		
		return null;
		
	}

}
