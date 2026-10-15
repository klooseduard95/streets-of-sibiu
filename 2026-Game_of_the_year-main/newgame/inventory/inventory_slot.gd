extends TextureRect

func _make_custom_tooltip(for_text: String) -> Object:
	var rich_text = RichTextLabel.new()
	
	rich_text.bbcode_enabled = true
	rich_text.text = for_text
	
	rich_text.fit_content = true
	rich_text.autowrap_mode = TextServer.AUTOWRAP_WORD
	rich_text.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	
	rich_text.custom_minimum_size = Vector2(400,0)
	
	return rich_text
