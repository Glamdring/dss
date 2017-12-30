When signing a PDF, one can specify a visible signature to be placed on the page(s). Since multiple signature fields are not well supported, in case a visible signature is needed on every page, stamps (images) are used instead.

To do that, just use the proper `SignatureImageParameters` set for the `signatureImageParameters` and `stampImageParameters` fields.

To pass these parameters from the demo app, XML can be entered into the two respective fields. A sample XML would look like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<signatureImageParameters>
   <imageDocument>
      <name>background_image.png</name>
   </imageDocument>
   <pagePlacement>SINGLE_PAGE</pagePlacement>
   <page>-1</page>
   <pageRange>
      <all>false</all>
      <excludeFirst>false</excludeFirst>
      <excludeFirstCount>0</excludeFirstCount>
      <excludeLast>false</excludeLast>
      <excludeLastCount>0</excludeLastCount>
   </pageRange>
   <xAxis>25.0</xAxis>
   <yAxis>15.0</yAxis>
   <width>100</width>
   <height>30</height>
   <zoom>100</zoom>
   <signerTextImageVerticalAlignment>MIDDLE</signerTextImageVerticalAlignment>
   <textParameters>
      <signerNamePosition>FOREGROUND</signerNamePosition>
      <signerTextHorizontalAlignment>LEFT</signerTextHorizontalAlignment>
      <text>%CN_1%&#xA;%CN_2%%CN_3%</text>
      <font>
         <name>helvetica</name>
         <size>24</size>
         <type>NORMAL</type>
      </font>
   </textParameters>
   <textRightParameters>
      <signerNamePosition>FOREGROUND</signerNamePosition>
      <signerTextHorizontalAlignment>LEFT</signerTextHorizontalAlignment>
      <text>Signature created byTestDate: %DateTimeWithTimeZone%</text>
      <font>
         <name>helvetica</name>
         <size>15</size>
         <type>NORMAL</type>
      </font>
   </textRightParameters>
   <dateFormat>dd.MM.yyyy HH:mm ZZZ</dateFormat>
</signatureImageParameters> 
```

Below is a list of parameters that can be specified:

- `image` - This is the image to be used in the signature. It is specified when the java API is used and can be a `FileDocument` or an `InMemoryDocument`. It cannot be specified when using web service. 
- `imageDocument` - Same as above, but for web services. You can specify the full image (including data), or just the `name`, which is then looked-up in a preconfigured folder on the server.
- `pagePlacement` - Specifies on which pages the image is placed. Can be one of: `SINGLE_PAGE`, `ALL_PAGES`, `RANGE`.  
`page` - The page number, if `SINGLE_PAGE` is passed in the above parameter. Should always be used for the signature parameters and rarely be used for stamps. Negative values are counted from the end of the document, e.g. -1 means the last page.
`pageRange` - The page range (mostly used for stamps rather than signatures). You can specify a list of pages with potential exclusions (look at the object for more details).
- `xAxis`, `yAxis` - The coordinate of the signature/stamp. For convenience, the y coordinate starts from the bottom (as signatures are most often placed at the bottom).
- `width`, `height` - The size of the signature/stamp. They can be left blank, in which case the size is calculated based on the text, but it is not recommended to leave them blank.
- `zoom` - whether any zoom should be applied. Should not be specified normally.
- `backgroundColor` - The background color
- `dpi` - Supplied in case a custom DPI is needed for placement calculations. Should normally be left blank.
- `signerTextImageVerticalAlignment`, `alignmentHorizontal`, `alignmentVertical`
- `rotation` - Supplied in case any rotation should be applied to the image
- `textParameters` - Parameters about the contents and placement of the main text (if right text is specified, this goes on the left). It is a nested object and it has the followign self-explanatory parameters: `signerNamePosition` (where does the text stand in relation to the image: `TOP`, `BOTTOM`, `RIGHT`, `LEFT`, `FOREGROUND`), `signerTextHorizontalAlignment` (`LEFT`, `CENTER`, `RIGHT`), `text`, `font`, `textColor`. Check the object/XSD or the example above for how to specify them. The `text` fields supports the `%CN_X%` placeholder, where X can be 1, 2 or 3, to get the names of the signer. 
- `textRightParameters` - Same as above but for the text on the right of the image (if any)
- `dateFormat` - The format of the date in case the `%DateTimeWithTimeZone%` placeholder is used in the text
- `backgroundOpacity` - Opacity of the background image, in case it is placed in the background (and not on the side) of the text. 0 means fully opaque, 255 means transparent. It is practically used to make the background image fade in the background.