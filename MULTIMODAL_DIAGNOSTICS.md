# Multimodal Diagnostics - Vision Analysis

## Overview

Synthesis now supports multimodal diagnostic analysis using Claude 3 Sonnet's vision capabilities. Healthcare providers can upload medical images (X-rays, skin lesions, etc.) alongside clinical context to receive AI-powered image analysis and findings.

## Features

- **Image Analysis**: Upload JPEG or PNG medical images for AI analysis
- **Clinical Context Integration**: Provide patient symptoms and history for contextualized analysis
- **Structured Findings**: Receive detailed descriptions and key clinical findings
- **Error Handling**: Comprehensive validation for file types, sizes, and corrupted images

## API Endpoint

### POST /api/v1/diagnostic/analyze-image

Analyzes a medical image using Claude 3 Sonnet's vision capabilities.

**Authentication**: Required (Bearer token)

**Content-Type**: `multipart/form-data`

**Parameters**:
- `image` (required): Medical image file (JPEG or PNG)
- `clinicalContext` (optional): Clinical context or patient symptoms

**Constraints**:
- Supported formats: JPEG, PNG
- Maximum file size: 5MB
- Image must not be corrupted

**Example Request** (using curl):

```bash
curl -X POST http://localhost:8080/api/v1/diagnostic/analyze-image \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "image=@chest_xray.jpg" \
  -F "clinicalContext=Patient has 3-week persistent cough and fever"
```

**Example Response** (200 OK):

```json
{
  "description": "The chest X-ray demonstrates bilateral infiltrates predominantly in the lower lobes. The cardiac silhouette appears normal in size. No pleural effusion is evident. The findings are consistent with bilateral pneumonia.",
  "findings": [
    "Bilateral lower lobe infiltrates",
    "Normal cardiac silhouette",
    "No pleural effusion",
    "Findings suggestive of pneumonia"
  ],
  "analyzedAt": "2026-02-17T14:30:00"
}
```

**Error Responses**:

- **400 Bad Request**: Invalid file type or file too large
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Invalid file type. Only JPEG and PNG images are supported.",
    "httpStatus": 400,
    "timestamp": "2026-02-17T14:30:00"
  }
  ```

- **500 Internal Server Error**: Image analysis failed or corrupted file
  ```json
  {
    "code": "DIAGNOSTIC_ERROR",
    "message": "Failed to analyze medical image: The file may be corrupted.",
    "httpStatus": 500,
    "timestamp": "2026-02-17T14:30:00"
  }
  ```

## Technical Implementation

### Architecture

1. **BedrockClient.invokeVision()**: 
   - Accepts image bytes, media type, and text prompt
   - Encodes image to base64
   - Constructs Claude 3 messages API payload with image and text content blocks
   - Handles AWS SDK exceptions (throttling, oversized images)

2. **DiagnosticService.analyzeImage()**:
   - Validates file type (JPEG/PNG only)
   - Validates file size (max 5MB)
   - Builds clinical prompt for image analysis
   - Parses structured JSON response with description and findings

3. **DiagnosticController.analyzeImage()**:
   - Handles multipart file upload
   - Validates file is not empty
   - Extracts bytes and media type
   - Returns structured ImageAnalysisResponse

### AWS Bedrock Integration

**Model**: `anthropic.claude-3-sonnet-20240229-v1:0`

**Request Payload Structure**:
```json
{
  "anthropic_version": "bedrock-2023-05-31",
  "max_tokens": 2048,
  "temperature": 0.2,
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "<BASE64_ENCODED_IMAGE>"
          }
        },
        {
          "type": "text",
          "text": "Analyze this medical image..."
        }
      ]
    }
  ]
}
```

### Error Handling

- **File Validation**: Checks for supported formats (JPEG/PNG) and size limits (5MB)
- **AWS SDK Errors**: Catches `SdkClientException` for throttling or oversized images
- **Corrupted Files**: Catches `IOException` when reading file bytes
- **Parsing Errors**: Graceful fallback to raw text if JSON parsing fails

## Use Cases

### 1. Chest X-Ray Analysis
```bash
curl -X POST http://localhost:8080/api/v1/diagnostic/analyze-image \
  -H "Authorization: Bearer TOKEN" \
  -F "image=@chest_xray.jpg" \
  -F "clinicalContext=45-year-old male with fever, cough, and shortness of breath for 5 days"
```

### 2. Skin Lesion Assessment
```bash
curl -X POST http://localhost:8080/api/v1/diagnostic/analyze-image \
  -H "Authorization: Bearer TOKEN" \
  -F "image=@skin_lesion.png" \
  -F "clinicalContext=Patient noticed growing mole on left arm over 3 months"
```

### 3. Abdominal Imaging
```bash
curl -X POST http://localhost:8080/api/v1/diagnostic/analyze-image \
  -H "Authorization: Bearer TOKEN" \
  -F "image=@abdominal_xray.jpg" \
  -F "clinicalContext=Child with abdominal pain and vomiting"
```

## Testing

### Unit Tests

- **BedrockVisionClientTest**: Tests vision invocation with JPEG/PNG, unsupported formats, and SDK errors
- **ImageAnalysisServiceTest**: Tests validation, parsing, and error handling

### Integration Testing

To test with a real image:

```bash
# 1. Register and get token
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Dr. Test","role":"Doctor","clinicName":"Test","region":"North","email":"test@example.com","password":"password"}' \
  | jq -r '.token')

# 2. Upload image for analysis
curl -X POST http://localhost:8080/api/v1/diagnostic/analyze-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@test_xray.jpg" \
  -F "clinicalContext=Test patient with respiratory symptoms"
```

## Best Practices

1. **Image Quality**: Ensure images are clear and properly oriented
2. **Clinical Context**: Always provide relevant patient history for better analysis
3. **File Size**: Compress large images before upload (max 5MB)
4. **Format**: Use JPEG for photographs, PNG for diagrams/screenshots
5. **Privacy**: Ensure images are de-identified (no patient names/IDs visible)

## Limitations

- Maximum image size: 5MB
- Supported formats: JPEG, PNG only
- Analysis is AI-assisted, not a replacement for professional radiologist review
- Response time depends on image size and AWS Bedrock availability

## Future Enhancements

- Support for DICOM medical imaging format
- Batch image analysis
- Image comparison (before/after)
- Integration with PACS systems
- Automated report generation
- Multi-image analysis (multiple views)

## Security Considerations

- All image uploads require JWT authentication
- Images are not stored permanently (processed in-memory)
- Base64 encoding ensures safe transmission
- File type validation prevents malicious uploads
- Size limits prevent DoS attacks

## Troubleshooting

### "Invalid file type" Error
- Ensure file is JPEG or PNG format
- Check file extension matches actual format

### "Image file is too large" Error
- Compress image to under 5MB
- Use image optimization tools

### "Failed to analyze medical image" Error
- Check AWS credentials are configured
- Verify Bedrock model access in AWS region
- Check CloudWatch logs for detailed error messages

### "The file may be corrupted" Error
- Verify image file is not corrupted
- Try opening image in image viewer first
- Re-export image from source application

## Support

For issues or questions:
- Check application logs: `logs/synthesis.log`
- Review AWS CloudWatch logs for Bedrock errors
- Contact development team with error details and sample images (de-identified)
