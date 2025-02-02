////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

/*
 * Written by Jeff Dyer
 * Copyright (c) 1998-2003 Mountain View Compiler Company
 * All rights reserved.
 */

package macromedia.asc.parser;

/**
 * Node
 *
 * @author Jeff Dyer
 */
public interface States
{
	public static final int start_state = 0;
	public static final int error_state = start_state - 1;

	public static final int minusminus_state = start_state + 1;
	public static final int minusequal_state = minusminus_state + 1;
	public static final int minus_state = minusequal_state + 1;
	public static final int notequals_state = minus_state + 1;
	public static final int not_state = notequals_state + 1;
	public static final int remainderequal_state = not_state + 1;
	public static final int remainder_state = remainderequal_state + 1;
	public static final int logicaland_state = remainder_state + 1;
	public static final int logicalandassign_state = logicaland_state + 1;
	public static final int andequal_state = logicalandassign_state + 1;
	public static final int and_state = andequal_state + 1;
	public static final int leftparen_state = and_state + 1;
	public static final int rightparen_state = leftparen_state + 1;
	public static final int starequal_state = rightparen_state + 1;
	public static final int star_state = starequal_state + 1;
	public static final int comma_state = star_state + 1;
	public static final int dot_state = comma_state + 1;
	public static final int doubledot_state = dot_state + 1;
	public static final int tripledot_state = doubledot_state + 1;
	public static final int slashequal_state = tripledot_state + 1;
	public static final int slash_state = slashequal_state + 1;
	public static final int colon_state = slash_state + 1;
	public static final int doublecolon_state = colon_state + 1;
	public static final int semicolon_state = doublecolon_state + 1;
	public static final int questionmark_state = semicolon_state + 1;
	public static final int leftbracket_state = questionmark_state + 1;
	public static final int rightbracket_state = leftbracket_state + 1;
	public static final int bitwisexorassign_state = rightbracket_state + 1;
	public static final int bitwisexor_state = bitwisexorassign_state + 1;
	public static final int logicalxor_state = bitwisexor_state + 1;
	public static final int logicalxorassign_state = logicalxor_state + 1;
	public static final int leftbrace_state = logicalxorassign_state + 1;
	public static final int logicalor_state = leftbrace_state + 1;
	public static final int logicalorassign_state = logicalor_state + 1;
	public static final int orequal_state = logicalorassign_state + 1;
	public static final int or_state = orequal_state + 1;
	public static final int rightbrace_state = or_state + 1;
	public static final int squiggle_state = rightbrace_state + 1;
	public static final int plusplus_state = squiggle_state + 1;
	public static final int plusequal_state = plusplus_state + 1;
	public static final int plus_state = plusequal_state + 1;
	public static final int leftshiftequal_state = plus_state + 1;
	public static final int leftshift_state = leftshiftequal_state + 1;
	public static final int lessthanorequal_state = leftshift_state + 1;
	public static final int lessthan_state = lessthanorequal_state + 1;
	public static final int equal_state = lessthan_state + 1;
	public static final int equalequal_state = equal_state + 1;
	public static final int greaterthanorequal_state = equalequal_state + 1;
	public static final int rightshiftequal_state = greaterthanorequal_state + 1;
	public static final int unsignedrightshift_state = rightshiftequal_state + 1;
	public static final int unsignedrightshiftequal_state = unsignedrightshift_state + 1;
	public static final int rightshift_state = unsignedrightshiftequal_state + 1;
	public static final int greaterthan_state = rightshift_state + 1;
	public static final int A_state = greaterthan_state + 1;
	public static final int N_state = A_state + 1;
	public static final int AA_state = N_state + 1;
	public static final int AN_state = AA_state + 1;
	public static final int singlequote_state = AN_state + 1;
	public static final int doublequote_state = singlequote_state + 1;
	public static final int zero_state = doublequote_state + 1;
	public static final int decimalinteger_state = zero_state + 1;
	public static final int decimal_state = decimalinteger_state + 1;
	public static final int exponentstart_state = decimal_state + 1;
	public static final int exponent_state = exponentstart_state + 1;
	public static final int hexinteger_state = exponent_state + 1;
	public static final int slashregexp_state = hexinteger_state + 1;
	public static final int slashdiv_state = slashregexp_state + 1;
	public static final int regexp_state = slashdiv_state + 1;
	public static final int ampersand_state = regexp_state + 1;
	public static final int ampersandA_state = ampersand_state + 1;
	public static final int arrow_state = ampersandA_state + 1;

	public static final int a_state = arrow_state + 1;
	public static final int ab_state = a_state + 1;
	public static final int abs_state = ab_state + 1;
	public static final int abst_state = abs_state + 1;
	public static final int abstr_state = abst_state + 1;
	public static final int abstra_state = abstr_state + 1;
	public static final int abstrac_state = abstra_state + 1;
	public static final int abstract_state = abstrac_state + 1;
	public static final int as_state = abstract_state + 1;
	public static final int at_state = as_state + 1;
	public static final int att_state = at_state + 1;
	public static final int attr_state = att_state + 1;
	public static final int attri_state = attr_state + 1;
	public static final int attrib_state = attri_state + 1;
	public static final int attribu_state = attrib_state + 1;
	public static final int attribut_state = attribu_state + 1;
	public static final int attribute_state = attribut_state + 1;
	public static final int b_state = attribute_state + 1;
	public static final int bo_state = b_state + 1;
	public static final int boo_state = bo_state + 1;
	public static final int bool_state = boo_state + 1;
	public static final int boole_state = bool_state + 1;
	public static final int boolea_state = boole_state + 1;
	public static final int boolean_state = boolea_state + 1;
	public static final int br_state = boolean_state + 1;
	public static final int bre_state = br_state + 1;
	public static final int brea_state = bre_state + 1;
	public static final int break_state = brea_state + 1;
	public static final int by_state = break_state + 1;
	public static final int byt_state = by_state + 1;
	public static final int byte_state = byt_state + 1;
	public static final int c_state = byte_state + 1;
	public static final int ca_state = c_state + 1;
	public static final int cas_state = ca_state + 1;
	public static final int case_state = cas_state + 1;
	public static final int cat_state = case_state + 1;
	public static final int catc_state = cat_state + 1;
	public static final int catch_state = catc_state + 1;
	public static final int ch_state = catch_state + 1;
	public static final int cha_state = ch_state + 1;
	public static final int char_state = cha_state + 1;
	public static final int cl_state = char_state + 1;
	public static final int cla_state = cl_state + 1;
	public static final int clas_state = cla_state + 1;
	public static final int class_state = clas_state + 1;
	public static final int co_state = class_state + 1;
	public static final int con_state = co_state + 1;
	public static final int cont_state = con_state + 1;
	public static final int conti_state = cont_state + 1;
	public static final int contin_state = conti_state + 1;
	public static final int continu_state = contin_state + 1;
	public static final int continue_state = continu_state + 1;
	public static final int cons_state = continue_state + 1;
	public static final int const_state = cons_state + 1;
	public static final int constr_state = const_state + 1;
	public static final int constru_state = constr_state + 1;
	public static final int construc_state = constru_state + 1;
	public static final int construct_state = construc_state + 1;
	public static final int constructo_state = construct_state + 1;
	public static final int constructor_state = constructo_state + 1;
    public static final int conf_state = constructor_state + 1;
    public static final int confi_state = conf_state + 1;
    public static final int config_state = confi_state + 1;
	public static final int d_state = config_state + 1;
	public static final int de_state = d_state + 1;
	public static final int deb_state = de_state + 1;
	public static final int debu_state = deb_state + 1;
	public static final int debug_state = debu_state + 1;
	public static final int debugg_state = debug_state + 1;
	public static final int debugge_state = debugg_state + 1;
	public static final int debugger_state = debugge_state + 1;
	public static final int def_state = debugger_state + 1;
	public static final int defa_state = def_state + 1;
	public static final int defau_state = defa_state + 1;
	public static final int defaul_state = defau_state + 1;
	public static final int default_state = defaul_state + 1;
	public static final int del_state = default_state + 1;
	public static final int dele_state = del_state + 1;
	public static final int delet_state = dele_state + 1;
	public static final int delete_state = delet_state + 1;
	public static final int do_state = delete_state + 1;
	public static final int dou_state = do_state + 1;
	public static final int doub_state = dou_state + 1;
	public static final int doubl_state = doub_state + 1;
	public static final int double_state = doubl_state + 1;
	public static final int e_state = double_state + 1;
	public static final int el_state = e_state + 1;
	public static final int els_state = el_state + 1;
	public static final int else_state = els_state + 1;
	public static final int en_state = else_state + 1;
	public static final int enu_state = en_state + 1;
	public static final int enum_state = enu_state + 1;
	public static final int ev_state = enum_state + 1;
	public static final int eva_state = ev_state + 1;
	public static final int eval_state = eva_state + 1;
	public static final int ex_state = eval_state + 1;
	public static final int ext_state = ex_state + 1;
	public static final int exte_state = ext_state + 1;
	public static final int exten_state = exte_state + 1;
	public static final int extend_state = exten_state + 1;
	public static final int extends_state = extend_state + 1;
	public static final int f_state = extends_state + 1;
	public static final int fa_state = f_state + 1;
	public static final int fal_state = fa_state + 1;
	public static final int fals_state = fal_state + 1;
	public static final int false_state = fals_state + 1;
	public static final int fi_state = false_state + 1;
	public static final int fin_state = fi_state + 1;
	public static final int fina_state = fin_state + 1;
	public static final int final_state = fina_state + 1;
	public static final int finall_state = final_state + 1;
	public static final int finally_state = finall_state + 1;
	public static final int fl_state = finally_state + 1;
	public static final int flo_state = fl_state + 1;
	public static final int floa_state = flo_state + 1;
	public static final int float_state = floa_state + 1;
	public static final int fo_state = float_state + 1;
	public static final int for_state = fo_state + 1;
	public static final int fu_state = for_state + 1;
	public static final int fun_state = fu_state + 1;
	public static final int func_state = fun_state + 1;
	public static final int funct_state = func_state + 1;
	public static final int functi_state = funct_state + 1;
	public static final int functio_state = functi_state + 1;
	public static final int function_state = functio_state + 1;
	public static final int g_state = function_state + 1;
	public static final int ge_state = g_state + 1;
	public static final int get_state = ge_state + 1;
	public static final int go_state = get_state + 1;
	public static final int got_state = go_state + 1;
	public static final int goto_state = got_state + 1;
	public static final int i_state = goto_state + 1;
	public static final int if_state = i_state + 1;
	public static final int im_state = if_state + 1;
	public static final int imp_state = im_state + 1;
	public static final int impl_state = imp_state + 1;
	public static final int imple_state = impl_state + 1;
	public static final int implem_state = imple_state + 1;
	public static final int impleme_state = implem_state + 1;
	public static final int implemen_state = impleme_state + 1;
	public static final int implement_state = implemen_state + 1;
	public static final int implements_state = implement_state + 1;
	public static final int impo_state = implements_state + 1;
	public static final int impor_state = impo_state + 1;
	public static final int import_state = impor_state + 1;
	public static final int pound_state = import_state + 1;
	public static final int pound_i_state = pound_state + 1;
	public static final int pound_in_state = pound_i_state + 1;
	public static final int pound_inc_state = pound_in_state + 1;
	public static final int pound_incl_state = pound_inc_state + 1;
	public static final int pound_inclu_state = pound_incl_state + 1;
	public static final int pound_includ_state = pound_inclu_state + 1;
	public static final int pound_include_state = pound_includ_state + 1;
	public static final int in_state = pound_include_state + 1;
	public static final int inc_state = in_state + 1;
	public static final int incl_state = inc_state + 1;
	public static final int inclu_state = incl_state + 1;
	public static final int includ_state = inclu_state + 1;
	public static final int include_state = includ_state + 1;
	public static final int ins_state = include_state + 1;
	public static final int inst_state = ins_state + 1;
	public static final int insta_state = inst_state + 1;
	public static final int instan_state = insta_state + 1;
	public static final int instanc_state = instan_state + 1;
	public static final int instance_state = instanc_state + 1;
	public static final int instanceo_state = instance_state + 1;
	public static final int instanceof_state = instanceo_state + 1;
	public static final int int_state = instanceof_state + 1;
	public static final int inte_state = int_state + 1;
	public static final int inter_state = inte_state + 1;
	public static final int interf_state = inter_state + 1;
	public static final int interfa_state = interf_state + 1;
	public static final int interfac_state = interfa_state + 1;
	public static final int interface_state = interfac_state + 1;
	public static final int is_state = interface_state + 1;
	public static final int l_state = is_state + 1;
	public static final int lo_state = l_state + 1;
	public static final int lon_state = lo_state + 1;
	public static final int long_state = lon_state + 1;
	public static final int n_state = long_state + 1;
	public static final int na_state = n_state + 1;
	public static final int nam_state = na_state + 1;
	public static final int name_state = nam_state + 1;
	public static final int names_state = name_state + 1;
	public static final int namesp_state = names_state + 1;
	public static final int namespa_state = namesp_state + 1;
	public static final int namespac_state = namespa_state + 1;
	public static final int namespace_state = namespac_state + 1;
	public static final int nat_state = namespace_state + 1;
	public static final int nati_state = nat_state + 1;
	public static final int nativ_state = nati_state + 1;
	public static final int native_state = nativ_state + 1;
	public static final int ne_state = native_state + 1;
	public static final int new_state = ne_state + 1;
	public static final int nu_state = new_state + 1;
	public static final int nul_state = nu_state + 1;
	public static final int null_state = nul_state + 1;
	public static final int r_state = null_state + 1;
	public static final int re_state = r_state + 1;
	public static final int ret_state = re_state + 1;
	public static final int retu_state = ret_state + 1;
	public static final int retur_state = retu_state + 1;
	public static final int return_state = retur_state + 1;
	public static final int p_state = return_state + 1;
	public static final int pa_state = p_state + 1;
	public static final int pac_state = pa_state + 1;
	public static final int pack_state = pac_state + 1;
	public static final int packa_state = pack_state + 1;
	public static final int packag_state = packa_state + 1;
	public static final int package_state = packag_state + 1;
	public static final int pr_state = package_state + 1;
	public static final int pri_state = pr_state + 1;
	public static final int priv_state = pri_state + 1;
	public static final int priva_state = priv_state + 1;
	public static final int privat_state = priva_state + 1;
	public static final int private_state = privat_state + 1;
	public static final int pro_state = private_state + 1;
	public static final int prot_state = pro_state + 1;
	public static final int prote_state = prot_state + 1;
	public static final int protec_state = prote_state + 1;
	public static final int protect_state = protec_state + 1;
	public static final int protecte_state = protect_state + 1;
	public static final int protected_state = protecte_state + 1;
	public static final int pu_state = protected_state + 1;
	public static final int pub_state = pu_state + 1;
	public static final int publ_state = pub_state + 1;
	public static final int publi_state = publ_state + 1;
	public static final int public_state = publi_state + 1;
	public static final int s_state = public_state + 1;
	public static final int se_state = s_state + 1;
	public static final int set_state = se_state + 1;
	public static final int sh_state = set_state + 1;
	public static final int sho_state = sh_state + 1;
	public static final int shor_state = sho_state + 1;
	public static final int short_state = shor_state + 1;
	public static final int st_state = short_state + 1;
	public static final int sta_state = st_state + 1;
	public static final int stat_state = sta_state + 1;
	public static final int stati_state = stat_state + 1;
	public static final int static_state = stati_state + 1;
	public static final int su_state = static_state + 1;
	public static final int sup_state = su_state + 1;
	public static final int supe_state = sup_state + 1;
	public static final int super_state = supe_state + 1;
	public static final int sw_state = super_state + 1;
	public static final int swi_state = sw_state + 1;
	public static final int swit_state = swi_state + 1;
	public static final int switc_state = swit_state + 1;
	public static final int switch_state = switc_state + 1;
	public static final int sy_state = switch_state + 1;
	public static final int syn_state = sy_state + 1;
	public static final int sync_state = syn_state + 1;
	public static final int synch_state = sync_state + 1;
	public static final int synchr_state = synch_state + 1;
	public static final int synchro_state = synchr_state + 1;
	public static final int synchron_state = synchro_state + 1;
	public static final int synchroni_state = synchron_state + 1;
	public static final int synchroniz_state = synchroni_state + 1;
	public static final int synchronize_state = synchroniz_state + 1;
	public static final int synchronized_state = synchronize_state + 1;
	public static final int t_state = synchronized_state + 1;
	public static final int th_state = t_state + 1;
	public static final int thi_state = th_state + 1;
	public static final int this_state = thi_state + 1;
	public static final int thr_state = this_state + 1;
	public static final int thro_state = thr_state + 1;
	public static final int throw_state = thro_state + 1;
	public static final int throws_state = throw_state + 1;
	public static final int tr_state = throws_state + 1;
	public static final int tra_state = tr_state + 1;
	public static final int tran_state = tra_state + 1;
	public static final int trans_state = tran_state + 1;
	public static final int transi_state = trans_state + 1;
	public static final int transie_state = transi_state + 1;
	public static final int transien_state = transie_state + 1;
	public static final int transient_state = transien_state + 1;
	public static final int tru_state = transient_state + 1;
	public static final int true_state = tru_state + 1;
	public static final int try_state = true_state + 1;
	public static final int ty_state = try_state + 1;
	public static final int typ_state = ty_state + 1;
	public static final int type_state = typ_state + 1;
	public static final int typeo_state = type_state + 1;
	public static final int typeof_state = typeo_state + 1;
	public static final int u_state = typeof_state + 1;
	public static final int us_state = u_state + 1;
	public static final int use_state = us_state + 1;
	public static final int v_state = use_state + 1;
	public static final int va_state = v_state + 1;
	public static final int var_state = va_state + 1;
	public static final int vo_state = var_state + 1;
	public static final int vol_state = vo_state + 1;
	public static final int vola_state = vol_state + 1;
	public static final int volat_state = vola_state + 1;
	public static final int volati_state = volat_state + 1;
	public static final int volatil_state = volati_state + 1;
	public static final int volatile_state = volatil_state + 1;
	public static final int voi_state = volatile_state + 1;
	public static final int void_state = voi_state + 1;
	public static final int w_state = void_state + 1;
	public static final int wh_state = w_state + 1;
	public static final int whi_state = wh_state + 1;
	public static final int whil_state = whi_state + 1;
	public static final int while_state = whil_state + 1;
	public static final int wi_state = while_state + 1;
	public static final int wit_state = wi_state + 1;
    public static final int with_state = wit_state + 1;
    public static final int x_state = with_state + 1;
    public static final int xm_state = x_state + 1;
    public static final int xml_state = xm_state + 1;

	public static final int blockcommentstart_state = xml_state+1;
	public static final int blockcomment_state = blockcommentstart_state+1;
	public static final int blockcommentstar_state = blockcomment_state+1;
	public static final int doccomment_state = blockcommentstar_state+1;
	public static final int doccommentstar_state = doccomment_state+1;
	public static final int doccommenttag_state = doccommentstar_state+1;
	public static final int doccommentvalue_state = doccommenttag_state+1;
	public static final int linecomment_state = doccommentvalue_state+1;

	public static final int startxml_state = linecomment_state+1;
	public static final int startxmlname_state = startxml_state+1;
	public static final int xmlliteral_state = startxmlname_state+1;
	public static final int endxml_state = xmlliteral_state+1;
	public static final int endxmlname_state = endxml_state+1;
    
	public static final int xmlcommentorcdatastart_state = endxmlname_state+1;
	public static final int xmlcdatastart_state = xmlcommentorcdatastart_state+1;
	public static final int xmlcdatac_state = xmlcdatastart_state+1;
	public static final int xmlcdatacd_state = xmlcdatac_state+1;
	public static final int xmlcdatacda_state = xmlcdatacd_state+1;
	public static final int xmlcdatacdat_state = xmlcdatacda_state+1;
	public static final int xmlcdatacdata_state = xmlcdatacdat_state+1;
	public static final int xmlcdata_state = xmlcdatacdata_state+1;
	public static final int xmlcdataendstart_state = xmlcdata_state+1;    
	public static final int xmlcdataend_state = xmlcdataendstart_state+1;
    
	public static final int xmlcommentstart_state = xmlcdataend_state+1;
	public static final int xmlcomment_state = xmlcommentstart_state+1;
	public static final int xmlcommentendstart_state = xmlcomment_state+1;
	public static final int xmlcommentend_state = xmlcommentendstart_state+1;
    
	public static final int xmlpi_state = xmlcommentend_state+1;
	public static final int xmlpiend_state = xmlpi_state+1;

	public static final int xmltext_state = xmlpiend_state+1;
    
	public static final int utf8sig_state = xmltext_state+1;

	public static final int eol_state = utf8sig_state+1;
}
