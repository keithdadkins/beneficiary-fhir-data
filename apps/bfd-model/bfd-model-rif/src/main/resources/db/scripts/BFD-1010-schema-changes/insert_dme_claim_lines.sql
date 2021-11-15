insert into dme_claim_lines (
	parent_claim,
	clm_line_num,
	line_nch_pmt_amt,
	line_sbmtd_chrg_amt,
	line_alowd_chrg_amt,
	line_bene_ptb_ddctbl_amt,
	line_bene_pmt_amt,
	line_ndc_cd,
	line_cms_type_srvc_cd,
	line_coinsrnc_amt,
	line_icd_dgns_cd,
	line_icd_dgns_vrsn_cd,
	line_1st_expns_dt,
	line_hct_hgb_rslt_num,
	line_hct_hgb_type_cd,
	line_last_expns_dt,
	line_pmt_80_100_cd,
	line_place_of_srvc_cd,
	line_prmry_alowd_chrg_amt,
	line_bene_prmry_pyr_cd,
	line_prcsg_ind_cd,
	line_dme_prchs_price_amt,
	line_srvc_cnt,
	line_service_deductible,
	betos_cd,
	hcpcs_cd,
	hcpcs_4th_mdfr_cd,
	hcpcs_1st_mdfr_cd,
	hcpcs_2nd_mdfr_cd,
	hcpcs_3rd_mdfr_cd,
	dmerc_line_mtus_cd,
	dmerc_line_mtus_cnt,
	dmerc_line_prcng_state_cd,
	dmerc_line_scrn_svgs_amt,
	dmerc_line_supplr_type_cd,
	line_bene_prmry_pyr_pd_amt,
	prvdr_num,
	prvdr_npi,
	prvdr_spclty,
	prvdr_state_cd,
	prvdr_tax_num,
	prtcptng_ind_cd,
	line_prvdr_pmt_amt
)
select
	cast("parentClaim" as bigint),
	"lineNumber",
	"paymentAmount",
	"submittedChargeAmount",
	"allowedChargeAmount",
	"beneficiaryPartBDeductAmount",
	"beneficiaryPaymentAmount",
	"nationalDrugCode",
	"cmsServiceTypeCode",
	"coinsuranceAmount",
	"diagnosisCode",
	"diagnosisCodeVersion",
	"firstExpenseDate",
	"hctHgbTestResult",
	"hctHgbTestTypeCode",
	"lastExpenseDate",
	"paymentCode",
	"placeOfServiceCode",
	"primaryPayerAllowedChargeAmount",
	"primaryPayerCode",
	"processingIndicatorCode",
	"purchasePriceAmount",
	"serviceCount",
	"serviceDeductibleCode",
	"betosCode",
	"hcpcsCode",
	"hcpcsFourthModifierCode",
	"hcpcsInitialModifierCode",
	"hcpcsSecondModifierCode",
	"hcpcsThirdModifierCode",
	"mtusCode",
	"mtusCount",
	"pricingStateCode",
	"screenSavingsAmount",
	"supplierTypeCode",
	"primaryPayerPaidAmount",
	"providerBillingNumber",
	"providerNPI",
	"providerSpecialityCode",
	"providerStateCode",
	"providerTaxNumber",
	"providerParticipatingIndCode",
	"providerPaymentAmount"
from
	"DMEClaimLines"
on conflict on constraint
	dme_claim_lines_pkey
do nothing;