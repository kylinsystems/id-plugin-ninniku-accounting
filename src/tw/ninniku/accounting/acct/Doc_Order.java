package tw.ninniku.accounting.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;

import org.compiere.acct.Fact;
import org.compiere.model.MAcctSchema;

public class Doc_Order extends org.compiere.acct.Doc_Order {

	public Doc_Order(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public static Fact getCommitmentReleasePublic(MAcctSchema as, Doc_AllocationHdr doc_AllocationHdr,
			BigDecimal qtyInvoiced, int c_InvoiceLine_ID, BigDecimal percent) {

		return Doc_Order.getCommitmentRelease(as, doc_AllocationHdr,
				qtyInvoiced, c_InvoiceLine_ID, percent);
	}

}
