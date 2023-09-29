package tw.ninniku.accounting.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;
import tw.ninniku.accounting.model.ProductAcct;
import org.compiere.acct.Doc;
import org.compiere.acct.DocLine;
import org.compiere.acct.DocTax;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MCostDetail;
import org.compiere.model.ProductCost;
import org.compiere.util.Env;

public class Doc_Invoice extends org.compiere.acct.Doc_Invoice {

	public Doc_Invoice(MAcctSchema as, ResultSet rs, String trxName) {
		super(as, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public void load() {
		loadDocumentDetails();
	}

	@Override
	protected String loadDocumentDetails() {
		// TODO Auto-generated method stub
		return super.loadDocumentDetails();
	}

	@Override
	public ArrayList<Fact> createFacts(MAcctSchema as) {
		//
		ArrayList<Fact> facts = new ArrayList<Fact>();
		// create Fact Header
		Fact fact = new Fact(this, as, Fact.POST_Actual);

		// Cash based accounting
		if (!as.isAccrual())
			return facts;

		// ** ARI, ARF
		if (getDocumentType().equals(DOCTYPE_ARInvoice) || getDocumentType().equals(DOCTYPE_ARProForma)) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;

			// Header Charge CR
			BigDecimal amt = getAmount(Doc.AMTTYPE_Charge);
			if (amt != null && amt.signum() != 0)
				fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(), null, amt);
			// TaxDue CR
			for (int i = 0; i < m_taxes.length; i++) {
				amt = m_taxes[i].getAmount();
				if (amt != null && amt.signum() != 0) {
					FactLine tl = fact.createLine(null, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue, as),
							getC_Currency_ID(), null, amt);
					if (tl != null)
						tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
				}
			}
			// Revenue CR
			for (int i = 0; i < p_lines.length; i++) {
				amt = p_lines[i].getAmtSource();
				BigDecimal dAmt = null;
				if (as.isTradeDiscountPosted()) {
					BigDecimal discount = p_lines[i].getDiscount();
					if (discount != null && discount.signum() != 0) {
						amt = amt.add(discount);
						dAmt = discount;
						fact.createLine(p_lines[i], p_lines[i].getAccount(ProductCost.ACCTTYPE_P_TDiscountGrant, as),
								getC_Currency_ID(), dAmt, null);
					}
				}
				fact.createLine(p_lines[i], p_lines[i].getAccount(ProductCost.ACCTTYPE_P_Revenue, as),
						getC_Currency_ID(), null, amt);
				if (!p_lines[i].isItem()) {
					grossAmt = grossAmt.subtract(amt);
					serviceAmt = serviceAmt.add(amt);
				}
			}

			// Receivables DR
			int receivables_ID = getValidCombination_ID(Doc.ACCTTYPE_C_Receivable, as);
			int receivablesServices_ID = receivables_ID; // Receivable Services account Deprecated IDEMPIERE-362
			if (m_allLinesItem || !as.isPostServices() || receivables_ID == receivablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}
			if (grossAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), receivables_ID), getC_Currency_ID(), grossAmt, null);
			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), receivablesServices_ID), getC_Currency_ID(), serviceAmt,
						null);

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), true); // from Loc
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), false); // to Loc
				}
			}
		}
		// ARB Customer Return (Back)
		else if (getDocumentType().equals("RRC")) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;

			// Header Charge DR
			BigDecimal amt = getAmount(Doc.AMTTYPE_Charge);
			if (amt != null && amt.signum() != 0)
				fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(), amt, null);
			// TaxDue DR
			for (int i = 0; i < m_taxes.length; i++) {
				amt = m_taxes[i].getAmount();
				if (amt != null && amt.signum() != 0) {
					FactLine tl = fact.createLine(null, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue, as),
							getC_Currency_ID(), amt, null);
					if (tl != null)
						tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
				}
			}
			// Revenue CR
			// ARB account (RRC)
			MAcctSchemaDefault ad = as.getAcctSchemaDefault();
			MAccount rrcAcct = null;
			if (ad.get_ValueAsInt("P_Sales_Returns_Acct") > 0) {
				rrcAcct = MAccount.get(as.getCtx(), ad.get_ValueAsInt("P_Sales_Returns_Acct"));
			} else {
				rrcAcct = MAccount.get(as.getCtx(), getValidCombination_ID(ProductCost.ACCTTYPE_P_Revenue, as));
			}

			for (int i = 0; i < p_lines.length; i++) {
				amt = p_lines[i].getAmtSource();
				BigDecimal dAmt = null;
				if (as.isTradeDiscountPosted()) {
					BigDecimal discount = p_lines[i].getDiscount();
					if (discount != null && discount.signum() != 0) {
						amt = amt.add(discount);
						dAmt = discount;
						fact.createLine(p_lines[i], p_lines[i].getAccount(ProductCost.ACCTTYPE_P_TDiscountGrant, as),
								getC_Currency_ID(), null, dAmt);
					}
				}
				// return account
				fact.createLine(p_lines[i],
						ProductAcct.getAccout(p_lines[i].getM_Product_ID(), "P_Sales_Returns_Acct", as),
						getC_Currency_ID(), amt, null);

				if (!p_lines[i].isItem()) {
					grossAmt = grossAmt.subtract(amt);
					serviceAmt = serviceAmt.add(amt);
				}
			}
			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), true); // from Loc
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), false); // to Loc
				}
			}
			// Receivables CR
			int receivables_ID = getValidCombination_ID(Doc.ACCTTYPE_C_Receivable, as);
			int receivablesServices_ID = getValidCombination_ID(Doc.ACCTTYPE_C_Receivable_Services, as);
			if (m_allLinesItem || !as.isPostServices() || receivables_ID == receivablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}
			if (grossAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), receivables_ID), getC_Currency_ID(), null, grossAmt);
			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), receivablesServices_ID), getC_Currency_ID(), null,
						serviceAmt);
		}

		// ARC
		else if (getDocumentType().equals(DOCTYPE_ARCredit)) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;

			// Header Charge DR
			BigDecimal amt = getAmount(Doc.AMTTYPE_Charge);
			if (amt != null && amt.signum() != 0)
				fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(), amt, null);
			// TaxDue DR
			for (int i = 0; i < m_taxes.length; i++) {
				amt = m_taxes[i].getAmount();
				if (amt != null && amt.signum() != 0) {
					FactLine tl = fact.createLine(null, m_taxes[i].getAccount(DocTax.ACCTTYPE_TaxDue, as),
							getC_Currency_ID(), amt, null);
					if (tl != null)
						tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
				}
			}
			// Revenue CR
			for (int i = 0; i < p_lines.length; i++) {
				amt = p_lines[i].getAmtSource();
				BigDecimal dAmt = null;
				if (as.isTradeDiscountPosted()) {
					BigDecimal discount = p_lines[i].getDiscount();
					if (discount != null && discount.signum() != 0) {
						amt = amt.add(discount);
						dAmt = discount;
						fact.createLine(p_lines[i], p_lines[i].getAccount(ProductCost.ACCTTYPE_P_TDiscountGrant, as),
								getC_Currency_ID(), null, dAmt);
					}
				}
				fact.createLine(p_lines[i],
						ProductAcct.getAccout(p_lines[i].getM_Product_ID(), "P_Sales_Allowances_Acct", as),
						getC_Currency_ID(), amt, null);

				if (!p_lines[i].isItem()) {
					grossAmt = grossAmt.subtract(amt);
					serviceAmt = serviceAmt.add(amt);
				}
			}

			// Receivables CR
			int receivables_ID = getValidCombination_ID(Doc.ACCTTYPE_C_Receivable, as);
			int receivablesServices_ID = receivables_ID; // Receivable Services account Deprecated IDEMPIERE-362
			if (m_allLinesItem || !as.isPostServices() || receivables_ID == receivablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}
			if (grossAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), receivables_ID), getC_Currency_ID(), null, grossAmt);
			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), receivablesServices_ID), getC_Currency_ID(), null,
						serviceAmt);

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), true); // from Loc
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), false); // to Loc
				}
			}
		}

		// ** API
		else if (getDocumentType().equals(DOCTYPE_APInvoice)) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;

			// Charge DR
			fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(),
					getAmount(Doc.AMTTYPE_Charge), null);
			// TaxCredit DR
			for (int i = 0; i < m_taxes.length; i++) {
				FactLine tl = fact.createLine(null, m_taxes[i].getAccount(m_taxes[i].getAPTaxType(), as),
						getC_Currency_ID(), m_taxes[i].getAmount(), null);
				if (tl != null)
					tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
			}
			// Expense DR
			for (int i = 0; i < p_lines.length; i++) {
				DocLine line = p_lines[i];
				boolean landedCost = landedCost(as, fact, line, true);
				if (landedCost && as.isExplicitCostAdjustment()) {
					fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as), getC_Currency_ID(),
							line.getAmtSource(), null);
					//
					FactLine fl = fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as),
							getC_Currency_ID(), null, line.getAmtSource());
					String desc = line.getDescription();
					if (desc == null)
						desc = "100%";
					else
						desc += " 100%";
					fl.setDescription(desc);
				}
				if (!landedCost) {
					MAccount expense = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
					if (line.isItem())
						expense = line.getAccount(ProductCost.ACCTTYPE_P_InventoryClearing, as);
					BigDecimal amt = line.getAmtSource();
					BigDecimal dAmt = null;
					if (as.isTradeDiscountPosted() && !line.isItem()) {
						BigDecimal discount = line.getDiscount();
						if (discount != null && discount.signum() != 0) {
							amt = amt.add(discount);
							dAmt = discount;
							MAccount tradeDiscountReceived = line.getAccount(ProductCost.ACCTTYPE_P_TDiscountRec, as);
							fact.createLine(line, tradeDiscountReceived, getC_Currency_ID(), null, dAmt);
						}
					}
					fact.createLine(line, expense, getC_Currency_ID(), amt, null);
					if (!line.isItem()) {
						grossAmt = grossAmt.subtract(amt);
						serviceAmt = serviceAmt.add(amt);
					}
					//
					if (line.getM_Product_ID() != 0 && line.getProduct().isService()) // otherwise Inv Matching
						MCostDetail.createInvoice(as, line.getAD_Org_ID(), line.getM_Product_ID(),
								line.getM_AttributeSetInstance_ID(), line.get_ID(), 0, // No Cost Element
								line.getAmtSource(), line.getQty(), line.getDescription(), getTrxName());
				}
			}

			// Liability CR
			int payables_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as);
			int payablesServices_ID = payables_ID; // Liability Services account Deprecated IDEMPIERE-362
			if (m_allLinesItem || !as.isPostServices() || payables_ID == payablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}
			if (grossAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payables_ID), getC_Currency_ID(), null, grossAmt);
			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payablesServices_ID), getC_Currency_ID(), null,
						serviceAmt);

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), true); // from Loc
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), false); // to Loc
				}
			}

			//
			updateProductPO(as); // Only API
		}
		// APB (RPC) Vendor (Purchase ) Return
		// 進貨退回 Purchase Return
		else if (getDocumentType().equals("RPC")) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;
			// Charge CR
			fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(), null,
					getAmount(Doc.AMTTYPE_Charge));
			// TaxCredit CR
			for (int i = 0; i < m_taxes.length; i++) {
				FactLine tl = fact.createLine(null, m_taxes[i].getAccount(m_taxes[i].getAPTaxType(), as),
						getC_Currency_ID(), null, m_taxes[i].getAmount());
				if (tl != null)
					tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
			}
			// Expense CR
			for (int i = 0; i < p_lines.length; i++) {
				DocLine line = p_lines[i];
				boolean landedCost = landedCost(as, fact, line, false);
				if (landedCost && as.isExplicitCostAdjustment()) {
					fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as), getC_Currency_ID(), null,
							line.getAmtSource());
					//
					FactLine fl = fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as),
							getC_Currency_ID(), line.getAmtSource(), null);
					String desc = line.getDescription();
					if (desc == null)
						desc = "100%";
					else
						desc += " 100%";
					fl.setDescription(desc);
				}
				if (!landedCost) {
					MAccount expense = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
					if (line.isItem())
						expense = line.getAccount(ProductCost.ACCTTYPE_P_InventoryClearing, as);
					BigDecimal amt = line.getAmtSource();
					BigDecimal dAmt = null;
					if (as.isTradeDiscountPosted() && !line.isItem()) {
						BigDecimal discount = line.getDiscount();
						if (discount != null && discount.signum() != 0) {
							amt = amt.add(discount);
							dAmt = discount;
							MAccount tradeDiscountReceived = line.getAccount(ProductCost.ACCTTYPE_P_TDiscountRec, as);
							fact.createLine(line, tradeDiscountReceived, getC_Currency_ID(), dAmt, null);
						}
					}
					fact.createLine(line, expense, getC_Currency_ID(), null, amt);
					if (!line.isItem()) {
						grossAmt = grossAmt.subtract(amt);
						serviceAmt = serviceAmt.add(amt);
					}
					//
					if (line.getM_Product_ID() != 0 && line.getProduct().isService()) // otherwise Inv Matching
						MCostDetail.createInvoice(as, line.getAD_Org_ID(), line.getM_Product_ID(),
								line.getM_AttributeSetInstance_ID(), line.get_ID(), 0, // No Cost Element
								line.getAmtSource().negate(), line.getQty(), line.getDescription(), getTrxName());
				}
			}
			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), true); // from Loc
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), false); // to Loc
				}
			}
			// Liability DR
			int payables_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as);
			int payablesServices_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability_Services, as);
			if (m_allLinesItem || !as.isPostServices() || payables_ID == payablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}

			// APB (RPC) account
			MAcctSchemaDefault ad = as.getAcctSchemaDefault();
			MAccount rpcAcct = null;
			if (ad.get_ValueAsInt("V_Purchase_Returns_Acct") > 0) {
				rpcAcct = MAccount.get(as.getCtx(), ad.get_ValueAsInt("V_Purchase_Returns_Acct"));
			} else {
				rpcAcct = MAccount.get(as.getCtx(), getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as));
			}

			if (grossAmt.signum() != 0)
				fact.createLine(null, rpcAcct, getC_Currency_ID(), grossAmt, null);

			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payablesServices_ID), getC_Currency_ID(), serviceAmt,
						null);
		}
		//RPC
		else if (getDocumentType().equals("RPC")) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;
			// Charge CR
			fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(), null,
					getAmount(Doc.AMTTYPE_Charge));
			// TaxCredit CR
			for (int i = 0; i < m_taxes.length; i++) {
				FactLine tl = fact.createLine(null, m_taxes[i].getAccount(m_taxes[i].getAPTaxType(), as),
						getC_Currency_ID(), null, m_taxes[i].getAmount());
				if (tl != null)
					tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
			}
			// Expense CR
			for (int i = 0; i < p_lines.length; i++) {
				DocLine line = p_lines[i];
				boolean landedCost = landedCost(as, fact, line, false);
				if (landedCost && as.isExplicitCostAdjustment()) {
					fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as), getC_Currency_ID(), null,
							line.getAmtSource());
					//
					FactLine fl = fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as),
							getC_Currency_ID(), line.getAmtSource(), null);
					String desc = line.getDescription();
					if (desc == null)
						desc = "100%";
					else
						desc += " 100%";
					fl.setDescription(desc);
				}
				if (!landedCost) {
					MAccount expense = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
					if (line.isItem())
						expense = line.getAccount(ProductCost.ACCTTYPE_P_InventoryClearing, as);
					BigDecimal amt = line.getAmtSource();
					BigDecimal dAmt = null;
					if (as.isTradeDiscountPosted() && !line.isItem()) {
						BigDecimal discount = line.getDiscount();
						if (discount != null && discount.signum() != 0) {
							amt = amt.add(discount);
							dAmt = discount;
							MAccount tradeDiscountReceived = line.getAccount(ProductCost.ACCTTYPE_P_TDiscountRec, as);
							fact.createLine(line, tradeDiscountReceived, getC_Currency_ID(), dAmt, null);
						}
					}
					//進貨退回科目
					expense = ProductAcct.getAccout(line.getM_Product_ID(), "P_Purchase_Returns_Acct", as);
					fact.createLine(line, expense, getC_Currency_ID(), null, amt);
					if (!line.isItem()) {
						grossAmt = grossAmt.subtract(amt);
						serviceAmt = serviceAmt.add(amt);
					}
					//
					if (line.getM_Product_ID() != 0 && line.getProduct().isService()) // otherwise Inv Matching
						MCostDetail.createInvoice(as, line.getAD_Org_ID(), line.getM_Product_ID(),
								line.getM_AttributeSetInstance_ID(), line.get_ID(), 0, // No Cost Element
								line.getAmtSource().negate(), line.getQty(), line.getDescription(), getTrxName());
				}
			}

			// Liability DR
			int payables_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as);
			int payablesServices_ID = payables_ID; // Liability Services account Deprecated IDEMPIERE-362
			if (m_allLinesItem || !as.isPostServices() || payables_ID == payablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}
			if (grossAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payables_ID), getC_Currency_ID(), grossAmt, null);
			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payablesServices_ID), getC_Currency_ID(), serviceAmt,
						null);

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), true); // from Loc
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), false); // to Loc
				}
			}
		}
		// APC
		else if (getDocumentType().equals(DOCTYPE_APCredit)) {
			BigDecimal grossAmt = getAmount(Doc.AMTTYPE_Gross);
			BigDecimal serviceAmt = Env.ZERO;
			// Charge CR
			fact.createLine(null, getAccount(Doc.ACCTTYPE_Charge, as), getC_Currency_ID(), null,
					getAmount(Doc.AMTTYPE_Charge));
			// TaxCredit CR
			for (int i = 0; i < m_taxes.length; i++) {
				FactLine tl = fact.createLine(null, m_taxes[i].getAccount(m_taxes[i].getAPTaxType(), as),
						getC_Currency_ID(), null, m_taxes[i].getAmount());
				if (tl != null)
					tl.setC_Tax_ID(m_taxes[i].getC_Tax_ID());
			}
			// Expense CR
			for (int i = 0; i < p_lines.length; i++) {
				DocLine line = p_lines[i];
				boolean landedCost = landedCost(as, fact, line, false);
				if (landedCost && as.isExplicitCostAdjustment()) {
					fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as), getC_Currency_ID(), null,
							line.getAmtSource());
					//
					FactLine fl = fact.createLine(line, line.getAccount(ProductCost.ACCTTYPE_P_Expense, as),
							getC_Currency_ID(), line.getAmtSource(), null);
					String desc = line.getDescription();
					if (desc == null)
						desc = "100%";
					else
						desc += " 100%";
					fl.setDescription(desc);
				}
				if (!landedCost) {
					MAccount expense = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
					if (line.isItem())
						expense = line.getAccount(ProductCost.ACCTTYPE_P_InventoryClearing, as);
					BigDecimal amt = line.getAmtSource();
					BigDecimal dAmt = null;
					if (as.isTradeDiscountPosted() && !line.isItem()) {
						BigDecimal discount = line.getDiscount();
						if (discount != null && discount.signum() != 0) {
							amt = amt.add(discount);
							dAmt = discount;
							MAccount tradeDiscountReceived = line.getAccount(ProductCost.ACCTTYPE_P_TDiscountRec, as);
							fact.createLine(line, tradeDiscountReceived, getC_Currency_ID(), dAmt, null);
						}
					}
					//進貨折讓科目
					expense = ProductAcct.getAccout(line.getM_Product_ID(), "P_Purchase_Allowances_Acct", as);
					fact.createLine(line, expense, getC_Currency_ID(), null, amt);
					if (!line.isItem()) {
						grossAmt = grossAmt.subtract(amt);
						serviceAmt = serviceAmt.add(amt);
					}
					//
					if (line.getM_Product_ID() != 0 && line.getProduct().isService()) // otherwise Inv Matching
						MCostDetail.createInvoice(as, line.getAD_Org_ID(), line.getM_Product_ID(),
								line.getM_AttributeSetInstance_ID(), line.get_ID(), 0, // No Cost Element
								line.getAmtSource().negate(), line.getQty(), line.getDescription(), getTrxName());
				}
			}

			// Liability DR
			int payables_ID = getValidCombination_ID(Doc.ACCTTYPE_V_Liability, as);
			int payablesServices_ID = payables_ID; // Liability Services account Deprecated IDEMPIERE-362
			if (m_allLinesItem || !as.isPostServices() || payables_ID == payablesServices_ID) {
				grossAmt = getAmount(Doc.AMTTYPE_Gross);
				serviceAmt = Env.ZERO;
			} else if (m_allLinesService) {
				serviceAmt = getAmount(Doc.AMTTYPE_Gross);
				grossAmt = Env.ZERO;
			}
			if (grossAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payables_ID), getC_Currency_ID(), grossAmt, null);
			if (serviceAmt.signum() != 0)
				fact.createLine(null, MAccount.get(getCtx(), payablesServices_ID), getC_Currency_ID(), serviceAmt,
						null);

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromBPartner(getC_BPartner_Location_ID(), true); // from Loc
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(), false); // to Loc
				}
			}
		} else {
			p_Error = "DocumentType unknown: " + getDocumentType();
			log.log(Level.SEVERE, p_Error);
			fact = null;
		}
		//
		facts.add(fact);
		return facts;
	}

}
