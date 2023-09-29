package tw.ninniku.accounting.acct;

import java.math.BigDecimal;

import org.compiere.acct.Doc;
import org.compiere.model.MAllocationLine;

public class DocLine_Allocation extends org.compiere.acct.DocLine_Allocation {

	public DocLine_Allocation(MAllocationLine line, Doc doc) {
		super(line, doc);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void setC_ConversionType_ID(int C_ConversionType_ID) {
		// TODO Auto-generated method stub
		super.setC_ConversionType_ID(C_ConversionType_ID);
	}

	@Override
	protected void setCurrencyRate(BigDecimal currencyRate) {
		// TODO Auto-generated method stub
		super.setCurrencyRate(currencyRate);
	}
	
}
