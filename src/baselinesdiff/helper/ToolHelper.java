package baselinesdiff.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import baselinesdiff.DifferenceBaseline;
import difflib.Delta;
import difflib.DiffRow;
import difflib.DiffRow.Tag;
import difflib.DiffRowGenerator;
import difflib.DiffUtils;
import difflib.Patch;

public class ToolHelper {

//	public static void main(String[] args) throws Exception {
//		testCompare();
//
//	}

	public static void getDiff(List<String> R_BodyList, List<String> L_BodyList, String printStr) throws IOException {

		Patch patch = DiffUtils.diff(R_BodyList, L_BodyList);

		DiffRowGenerator.Builder builder = new DiffRowGenerator.Builder();
		builder.showInlineDiffs(false);
		DiffRowGenerator generator = builder.build();
		boolean addTitle = true;
		boolean delTitle = true;

		List<DiffRow> changRows = new ArrayList<DiffRow>();
		if (patch.getDeltas().size() > 0) {
			DifferenceBaseline.diffTxtHelper.writeText(printStr);
		}
		for (Delta delta : patch.getDeltas()) {

			List<DiffRow> generateDiffRows = generator.generateDiffRows((List<String>) delta.getOriginal().getLines(),
					(List<String>) delta.getRevised().getLines());
//			System.out.println("getDiff generateDiffRows:"+generateDiffRows);
			for (DiffRow row : generateDiffRows) {
				if (StringUtils.isEmpty(row.getNewLine()))
					row.setTag(Tag.DELETE);
//				if(StringUtils.isEmpty(row.getOldLine()))
//					row.setTag(Tag.INSERT);
			}

			int leftPos = delta.getOriginal().getPosition();
			int rightPos = delta.getRevised().getPosition();
			int count = 1;

			if (DifferenceBaseline.DecisionTableTitle.equals(printStr))
				count = 0;
//			System.out.println("generateDiffRows: "+generateDiffRows);
			for (DiffRow row : generateDiffRows) {

				Tag tag = row.getTag();
				if (tag == Tag.INSERT) {
					if (addTitle) {
						DifferenceBaseline.diffTxtHelper.writeText("新增規則 : ");
					}
					DifferenceBaseline.diffTxtHelper.writeText(
							"第" + String.format("%03d", (rightPos + count)) + "行 :" + trim(row.getNewLine()));

					addTitle = false;
					count++;
				} else if (tag == Tag.CHANGE) {
					row.setOldLine(
							"修改前 第" + String.format("%03d", (leftPos + count)) + "行 : " + trim(row.getOldLine()));
					row.setNewLine(
							"修改後 第" + String.format("%03d", (rightPos + count)) + "行 : " + trim(row.getNewLine()));

					changRows.add(row);
					count++;
				} else if (tag == Tag.DELETE) {
					if (delTitle) {
						DifferenceBaseline.diffTxtHelper.writeText("移除規則: ");
					}
					DifferenceBaseline.diffTxtHelper.writeText(
							"第" + String.format("%03d", (rightPos + count)) + "行 :" + trim(row.getOldLine()));

					delTitle = false;
					count++;
				} else if (tag == Tag.EQUAL) {
					System.out.println("equal: ");
					System.out.println("old-> " + row.getOldLine());
					System.out.println("new-> " + row.getNewLine());
					System.out.println("");
				} else {
					throw new IllegalStateException("Unknown pattern tag: " + tag);
				}
			}
		}
		if (changRows.size() > 0) {
			DifferenceBaseline.diffTxtHelper.writeText("更新異動: ");
		}
		int count = 0;
		for (DiffRow row : changRows) {
			count++;
			DifferenceBaseline.diffTxtHelper.writeText(row.getOldLine());
			DifferenceBaseline.diffTxtHelper.writeText(row.getNewLine());
			if (count != changRows.size())
				DifferenceBaseline.diffTxtHelper.writeText("\r");
		}

	}

	public static String trim(String in) {

		if (in == null || StringUtils.isEmpty(in))
			return "";

		return in.replaceAll("<br>", "");
	}
}